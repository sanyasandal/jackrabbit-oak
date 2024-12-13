package org.apache.jackrabbit.oak.upgrade;

import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.transform.Normalize;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.translate.Batchifier;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

import java.io.FileInputStream;
import java.io.InputStream;

public class ImageEmbeddingComparison {

  public static void compareImages(InputStream inputStream1, InputStream inputStream2) throws Exception {
    // Paths to your images
//    InputStream inputStream1 = new FileInputStream("path_to_image1.jpg");
//    InputStream inputStream2 = new FileInputStream("path_to_image2.jpg");

    // Load images using DJL ImageFactory
    Image img1 = ImageFactory.getInstance().fromInputStream(inputStream1);
    Image img2 = ImageFactory.getInstance().fromInputStream(inputStream2);


    // Load the pre-trained ResNet model for feature extraction (ResNet50 or ResNet18, etc.)
    Criteria<Image, NDArray> criteria = Criteria.builder()
      .setTypes(Image.class, NDArray.class)
      .optTranslator(new FeatureExtractionTranslator())
      .build();

    try (Model model = ModelZoo.loadModel(criteria)) {
      // Create a predictor for feature extraction
      try (Predictor<Image, NDArray> predictor = model.newPredictor(new FeatureExtractionTranslator())) {
        // Extract features for both images
        NDArray feature1 = predictor.predict(img1);
        NDArray feature2 = predictor.predict(img2);

        // Compute cosine similarity
        float similarity = computeCosineSimilarity(feature1, feature2);
        System.out.println("Cosine Similarity: " + similarity);
      }
    }
  }

  // Helper function to compute cosine similarity
  private static float computeCosineSimilarity(NDArray a, NDArray b) {
    float dotProduct = a.dot(b).getFloat();
    float normA = a.norm().getFloat();
    float normB = b.norm().getFloat();
    return dotProduct / (normA * normB);
  }

  // Translator for feature extraction using ResNet
  private static class FeatureExtractionTranslator implements Translator<Image, NDArray> {

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
      // Resize, convert to tensor, and normalize the image
      NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
      array = array.toType(DataType.FLOAT32, false);
      array = array.div(255f); // Normalize pixel values to [0, 1]
      // array = Resize.resize(array, new Shape(224, 224, 3));
      //array = Normalize.normalize(array, new float[]{0.485f, 0.456f, 0.406f},
      //  new float[]{0.229f, 0.224f, 0.225f});
      array = array.transpose(2, 0, 1); // Convert to CHW format for PyTorch
      return new NDList(array.expandDims(0)); // Add batch dimension
    }

    @Override
    public NDArray processOutput(TranslatorContext ctx, NDList list) {
      return list.singletonOrThrow(); // Extract the model output
    }

    @Override
    public Batchifier getBatchifier() {
      return null; // No batching needed
    }
  }
}
