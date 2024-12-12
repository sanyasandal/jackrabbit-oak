//package org.apache.jackrabbit.oak.upgrade.security;
//
//import ai.djl.Application;
//import ai.djl.ModelException;
//import ai.djl.inference.Predictor;
//import ai.djl.modality.cv.Image;
//import ai.djl.modality.cv.ImageFactory;
//import ai.djl.modality.cv.transform.Resize;
//import ai.djl.modality.cv.transform.ToTensor;
//import ai.djl.modality.cv.transform.Transforms;
//import ai.djl.ndarray.NDArray;
//import ai.djl.ndarray.NDList;
//import ai.djl.repository.zoo.Criteria;
//import ai.djl.repository.zoo.ModelZoo;
//import ai.djl.repository.zoo.ZooModel;
//import ai.djl.translate.Batchifier;
//import ai.djl.translate.TranslateException;
//import ai.djl.translate.Translator;
//import ai.djl.translate.TranslatorContext;
//import java.io.IOException;
//import java.io.InputStream;
//
//public class ImageEmbeddingComparison {
//
//  // Define the Translator to preprocess the image and extract embeddings
//  static class ImageTranslator implements Translator<Image, float[]> {
//
//    @Override
//    public NDArray processInput(TranslatorContext ctx, Image input) {
//      // Preprocess: Resize and convert to tensor
//      return Transforms.resize(input, 224, 224)
//        .toTensor(ctx.getNDManager());
//    }
//
//    @Override
//    public float[] processOutput(TranslatorContext ctx, NDArray output) {
//      // Convert the NDArray to a float array
//      return output.toFloatArray();
//    }
//
//    @Override
//    public Batchifier getBatchifier() {
//      return null; // No batching needed
//    }
//
//    @Override
//    public float[] processOutput(TranslatorContext translatorContext, NDList ndList) throws Exception {
//      return new float[0];
//    }
//  }
//
//  public static void compareImages(InputStream imageStream1, InputStream imageStream2) {
//    try {
//      // Load the pre-trained model (e.g., CLIP)
//      Criteria<Image, float[]> criteria = Criteria.builder()
//        .setTypes(Image.class, float[].class)
//        .optApplication(Application.CV.EMBEDDING)
//        .optEngine("PyTorch") // Ensure the PyTorch engine is being used
//        .optTranslator(new ImageTranslator())
//        .build();
//
//      try (ZooModel<Image, float[]> model = ModelZoo.loadModel(criteria)) {
//        // Load images from InputStreams
//        Image image1 = ImageFactory.getInstance().fromInputStream(imageStream1);
//        Image image2 = ImageFactory.getInstance().fromInputStream(imageStream2);
//
//        // Generate embeddings
//        try (Predictor<Image, float[]> predictor = model.newPredictor()) {
//          float[] embedding1 = predictor.predict(image1);
//          float[] embedding2 = predictor.predict(image2);
//
//          // Compute cosine similarity
//          float similarity = cosineSimilarity(embedding1, embedding2);
//          System.out.println("Cosine Similarity: " + similarity);
//        }
//      }
//    } catch (ModelException | TranslateException | IOException e) {
//      e.printStackTrace();
//    }
//  }
//
//  public static float cosineSimilarity(float[] vectorA, float[] vectorB) {
//    float dotProduct = 0f, normA = 0f, normB = 0f;
//    for (int i = 0; i < vectorA.length; i++) {
//      dotProduct += vectorA[i] * vectorB[i];
//      normA += Math.pow(vectorA[i], 2);
//      normB += Math.pow(vectorB[i], 2);
//    }
//    return dotProduct / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
//  }
//
//}
