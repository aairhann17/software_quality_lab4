package com.ontariotechu.sofe3980U;


import java.io.FileReader; 
import java.util.List;
import com.opencsv.*;

/**
 * Evaluates multiclass classification model outputs.
 *
 * Input CSV format:
 * - Column 0: true class index in {1, 2, 3, 4, 5}
 * - Columns 1..5: predicted class probabilities for classes 1..5
 *
 * This program computes cross-entropy (CE) and the confusion matrix.
 */
public class App 
{
	/**
	 * Container for multiclass metrics of one model file.
	 */
	private static class MultiClassMetrics {
		float crossEntropy;
		int[][] confusionMatrix;

		MultiClassMetrics(float crossEntropy, int[][] confusionMatrix) {
			this.crossEntropy = crossEntropy;
			this.confusionMatrix = confusionMatrix;
		}
	}

	/**
	 * Reads one CSV file and computes cross-entropy and confusion matrix.
	 *
	 * @param filePath input CSV path
	 * @return computed multiclass metrics
	 * @throws Exception when file reading or parsing fails
	 */
	private static MultiClassMetrics evaluateFile(String filePath) throws Exception {
		FileReader fileReader = new FileReader(filePath);
		CSVReader csvReader = new CSVReaderBuilder(fileReader).withSkipLines(1).build();
		List<String[]> allData = csvReader.readAll();
		csvReader.close();

		int classCount = 5;
		int[][] confusionMatrix = new int[classCount][classCount];
		float epsilon = 1.0e-7f;
		float ceSum = 0.0f;

		// Process each prediction row and accumulate CE + confusion counts.
		for (String[] row : allData) {
			int yTrue = Integer.parseInt(row[0]);
			float[] probabilities = new float[classCount];

			int predictedClass = 1;
			float bestProbability = Float.parseFloat(row[1]);

			for (int i = 0; i < classCount; i++) {
				probabilities[i] = Float.parseFloat(row[i + 1]);
				if (probabilities[i] > bestProbability) {
					bestProbability = probabilities[i];
					predictedClass = i + 1;
				}
			}

			// Clamp probability before log to avoid log(0).
			float trueClassProbability = Math.max(epsilon, Math.min(1.0f - epsilon, probabilities[yTrue - 1]));
			ceSum += -Math.log(trueClassProbability);

			// Rows represent predicted class, columns represent actual class.
			confusionMatrix[predictedClass - 1][yTrue - 1]++;
		}

		float crossEntropy = ceSum / allData.size();
		return new MultiClassMetrics(crossEntropy, confusionMatrix);
	}

	/**
	 * Prints CE and the 5x5 confusion matrix.
	 *
	 * @param metrics metrics to print
	 */
	private static void printMetrics(MultiClassMetrics metrics) {
		System.out.println("CE =" + metrics.crossEntropy);
		System.out.println("Confusion matrix");
		System.out.println("\t\ty=1\t\ty=2\t\ty=3\t\ty=4\t\ty=5");
		// Print one row per predicted class.
		for (int row = 0; row < 5; row++) {
			System.out.println("\ty^=" + (row + 1) + "\t" + metrics.confusionMatrix[row][0] + "\t" + metrics.confusionMatrix[row][1] + "\t" + metrics.confusionMatrix[row][2] + "\t" + metrics.confusionMatrix[row][3] + "\t" + metrics.confusionMatrix[row][4]);
		}
	}

    public static void main( String[] args )
    {
		// Evaluate the provided multiclass validation file.
		String filePath = "model.csv";
		try {
			MultiClassMetrics metrics = evaluateFile(filePath);
			printMetrics(metrics);
		}
		catch (Exception e) {
			System.out.println("Error reading the CSV file");
		}
	}
}
