package org.apache.jackrabbit.oak.upgrade;

import ai.djl.Application;
import ai.djl.Model;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.translate.Batchifier;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.translate.TranslatorFactory;

import java.io.IOException;
import java.io.InputStream;

public class ImageEmbeddingComparison {

  public static void compareImages(InputStream sourceStream, InputStream targetStream) throws IOException, ModelException, TranslateException {
    // Load the source and target streams (replace with actual streams)
//    InputStream sourceStream = getSourceStream(); // Replace with your actual stream
//    InputStream targetStream = getTargetStream(); // Replace with your actual stream

    // Pre-process the images from input streams
    Image sourceImage = ImageFactory.getInstance().fromInputStream(sourceStream);
    Image targetImage = ImageFactory.getInstance().fromInputStream(targetStream);

    // Load the pre-trained CLIP model
    String modelPath = "huggingface/clip-vit-base-patch16"; // Replace with actual Hugging Face model path

    // Load the model using PyTorch engine
    Model model = Model.newInstance(modelPath);  // Set application to IMAGE_CLASSIFICATION
    // Extract embeddings from both images using CLIP
    float[] sourceEmbedding = extractImageEmbedding(model, sourceImage);
    float[] targetEmbedding = extractImageEmbedding(model, targetImage);

    // Calculate cosine similarity between the image embeddings
    double similarity = cosineSimilarity(sourceEmbedding, targetEmbedding);
    System.out.println("Cosine Similarity: " + similarity);
  }

  private static float[] extractImageEmbedding(Model model, Image image) throws ModelException, TranslateException {
    Translator<Image, float[]> translator = new Translator<Image, float[]>() {
      @Override
      public NDList processInput(TranslatorContext ctx, Image input) {
        NDArray array = input.toNDArray(ctx.getNDManager());
        return new NDList(array);
      }

      @Override
      public float[] processOutput(TranslatorContext ctx, NDList list) {
        return list.singletonOrThrow().toFloatArray();
      }

      @Override
      public Batchifier getBatchifier() {
        return null; // No batching needed
      }
    };

    // Use the predictor to extract image embedding
    try (Predictor<Image, float[]> predictor = model.newPredictor(translator)) {
      return predictor.predict(image);
    }
  }

  // Method to calculate cosine similarity
  private static double cosineSimilarity(float[] vectorA, float[] vectorB) {
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;

    for (int i = 0; i < vectorA.length; i++) {
      dotProduct += vectorA[i] * vectorB[i];
      normA += Math.pow(vectorA[i], 2);
      normB += Math.pow(vectorB[i], 2);
    }

    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
  }
}
