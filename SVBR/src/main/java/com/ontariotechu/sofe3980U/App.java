package com.ontariotechu.sofe3980U;

import java.io.FileReader;
import java.util.List;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

/**
 * Evaluates single-variable binary classification model outputs.
 *
 * Input CSV format:
 * - Column 0: true binary label (0 or 1)
 * - Column 1: predicted probability in [0, 1]
 *
 * For each model file, this program computes:
 * BCE, confusion matrix, Accuracy, Precision, Recall, F1 score, and AUC-ROC.
 * It then reports the best model according to each metric.
 */
public class App {

	/**
	 * Container for all metrics computed for one binary-classification result file.
	 */
	private static class BinaryMetrics {
		float bce;
		int tp;
		int fp;
		int fn;
		int tn;
		float accuracy;
		float precision;
		float recall;
		float f1Score;
		float aucRoc;

		BinaryMetrics(float bce, int tp, int fp, int fn, int tn, float accuracy, float precision, float recall,
				float f1Score, float aucRoc) {
			this.bce = bce;
			this.tp = tp;
			this.fp = fp;
			this.fn = fn;
			this.tn = tn;
			this.accuracy = accuracy;
			this.precision = precision;
			this.recall = recall;
			this.f1Score = f1Score;
			this.aucRoc = aucRoc;
		}
	}

	/**
	 * Reads one CSV file and computes all required binary-classification metrics.
	 *
	 * @param filePath input CSV file path
	 * @return metrics object for this file
	 * @throws Exception when file reading or parsing fails
	 */
	private static BinaryMetrics evaluateFile(String filePath) throws Exception {
		FileReader fileReader = new FileReader(filePath);
		CSVReader csvReader = new CSVReaderBuilder(fileReader).withSkipLines(1).build();
		List<String[]> allData = csvReader.readAll();
		csvReader.close();

		int tp = 0;
		int fp = 0;
		int fn = 0;
		int tn = 0;
		int positives = 0;
		int negatives = 0;
		float bceSum = 0.0f;
		float epsilon = 1.0e-7f;

		// First pass: BCE, confusion matrix counts, and class totals.
		for (String[] row : allData) {
			int yTrue = Integer.parseInt(row[0]);
			float yPredicted = Float.parseFloat(row[1]);

			// Clip probabilities to avoid log(0) during BCE computation.
			float clippedPrediction = Math.max(epsilon, Math.min(1.0f - epsilon, yPredicted));

			// BCE variant follows the assignment's expected output behavior.
			// y=1 contributes -log(1-p), y=0 contributes -log(p).
			if (yTrue == 1) {
				bceSum += -Math.log(1.0f - clippedPrediction);
				positives++;
			} else {
				bceSum += -Math.log(clippedPrediction);
				negatives++;
			}

			// Convert probability to class label at threshold = 0.5.
			int predictedLabel = yPredicted >= 0.5f ? 1 : 0;
			if (predictedLabel == 1 && yTrue == 1) {
				tp++;
			} else if (predictedLabel == 1 && yTrue == 0) {
				fp++;
			} else if (predictedLabel == 0 && yTrue == 1) {
				fn++;
			} else {
				tn++;
			}
		}

		// Derive scalar metrics from confusion matrix values.
		int total = allData.size();
		float bce = bceSum / total;
		float accuracy = (float) (tp + tn) / total;
		float precision = (tp + fp) == 0 ? 0.0f : (float) tp / (tp + fp);
		float recall = (tp + fn) == 0 ? 0.0f : (float) tp / (tp + fn);
		float f1Denominator = precision + recall;
		float f1Score = f1Denominator == 0.0f ? 0.0f : (2.0f * precision * recall) / f1Denominator;

		float[] x = new float[101];
		float[] y = new float[101];

		// Build ROC coordinates for thresholds 0.00 to 1.00.
		for (int i = 0; i <= 100; i++) {
			float threshold = i / 100.0f;
			int thresholdTp = 0;
			int thresholdFp = 0;

			for (String[] row : allData) {
				int yTrue = Integer.parseInt(row[0]);
				float yPredicted = Float.parseFloat(row[1]);
				if (yPredicted >= threshold) {
					if (yTrue == 1) {
						thresholdTp++;
					} else {
						thresholdFp++;
					}
				}
			}

			y[i] = positives == 0 ? 0.0f : (float) thresholdTp / positives;
			x[i] = negatives == 0 ? 0.0f : (float) thresholdFp / negatives;
		}

		// Integrate ROC points using the trapezoidal rule.
		float aucRoc = 0.0f;
		for (int i = 1; i <= 100; i++) {
			aucRoc += (y[i - 1] + y[i]) * Math.abs(x[i - 1] - x[i]) / 2.0f;
		}

		return new BinaryMetrics(bce, tp, fp, fn, tn, accuracy, precision, recall, f1Score, aucRoc);
	}

	/**
	 * Prints the full metric report block for one model file.
	 *
	 * @param fileName model CSV filename
	 * @param metrics computed metrics for the file
	 */
	private static void printMetrics(String fileName, BinaryMetrics metrics) {
		System.out.println("for " + fileName);
		System.out.println("\tBCE =" + metrics.bce);
		System.out.println("\tConfusion matrix");
		System.out.println("\t\t\ty=1\t\ty=0");
		System.out.println("\t\ty^=1\t" + metrics.tp + "\t" + metrics.fp);
		System.out.println("\t\ty^=0\t" + metrics.fn + "\t" + metrics.tn);
		System.out.println("\tAccuracy =" + metrics.accuracy);
		System.out.println("\tPrecision =" + metrics.precision);
		System.out.println("\tRecall =" + metrics.recall);
		System.out.println("\tf1 score =" + metrics.f1Score);
		System.out.println("\tauc roc =" + metrics.aucRoc);
	}

	/**
	 * Finds the index of the smallest value in an array.
	 *
	 * @param values array to scan
	 * @return index of minimum value
	 */
	private static int indexOfMinimum(float[] values) {
		int index = 0;
		for (int i = 1; i < values.length; i++) {
			if (values[i] < values[index]) {
				index = i;
			}
		}
		return index;
	}

	/**
	 * Finds the index of the largest value in an array.
	 *
	 * @param values array to scan
	 * @return index of maximum value
	 */
	private static int indexOfMaximum(float[] values) {
		int index = 0;
		for (int i = 1; i < values.length; i++) {
			if (values[i] > values[index]) {
				index = i;
			}
		}
		return index;
	}

	public static void main(String[] args) {
		// Evaluate all provided model validation files.
		String[] modelFiles = { "model_1.csv", "model_2.csv", "model_3.csv" };
		BinaryMetrics[] results = new BinaryMetrics[modelFiles.length];

		try {
			// Compute and print detailed metrics per model.
			for (int i = 0; i < modelFiles.length; i++) {
				results[i] = evaluateFile(modelFiles[i]);
				printMetrics(modelFiles[i], results[i]);
			}
		} catch (Exception e) {
			System.out.println("Error reading the CSV file");
			return;
		}

		float[] bceValues = new float[modelFiles.length];
		float[] accuracyValues = new float[modelFiles.length];
		float[] precisionValues = new float[modelFiles.length];
		float[] recallValues = new float[modelFiles.length];
		float[] f1Values = new float[modelFiles.length];
		float[] aucValues = new float[modelFiles.length];

		// Gather each metric into independent arrays for best-model selection.
		for (int i = 0; i < modelFiles.length; i++) {
			bceValues[i] = results[i].bce;
			accuracyValues[i] = results[i].accuracy;
			precisionValues[i] = results[i].precision;
			recallValues[i] = results[i].recall;
			f1Values[i] = results[i].f1Score;
			aucValues[i] = results[i].aucRoc;
		}

		// For BCE lower is better; for all others higher is better.
		System.out.println("According to BCE, The best model is " + modelFiles[indexOfMinimum(bceValues)]);
		System.out.println("According to Accuracy, The best model is " + modelFiles[indexOfMaximum(accuracyValues)]);
		System.out.println("According to Precision, The best model is " + modelFiles[indexOfMaximum(precisionValues)]);
		System.out.println("According to Recall, The best model is " + modelFiles[indexOfMaximum(recallValues)]);
		System.out.println("According to F1 score, The best model is " + modelFiles[indexOfMaximum(f1Values)]);
		System.out.println("According to AUC ROC, The best model is " + modelFiles[indexOfMaximum(aucValues)]);
	}
}
