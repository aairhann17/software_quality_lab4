package com.ontariotechu.sofe3980U;


import java.io.FileReader; 
import java.util.List;
import com.opencsv.*;

/**
 * Evaluates single-variable continuous regression models.
 *
 * Input CSV format:
 * - Column 0: true value
 * - Column 1: predicted value
 *
 * This program computes MSE, MAE, and MARE for multiple model result files,
 * then reports the best model (lowest error) per metric.
 */
public class App 
{
	/**
	 * Simple container for continuous-regression metrics of one model file.
	 */
	private static class RegressionMetrics {
		float mse;
		float mae;
		float mare;

		RegressionMetrics(float mse, float mae, float mare) {
			this.mse = mse;
			this.mae = mae;
			this.mare = mare;
		}
	}

	/**
	 * Reads one CSV file and computes MSE, MAE, and MARE over all rows.
	 *
	 * Note: epsilon is added in the MARE denominator to avoid division by zero,
	 * and it is set to 1e-3 to match the expected lab output.
	 *
	 * @param filePath input CSV file path
	 * @return metrics object for the file
	 * @throws Exception when file reading or parsing fails
	 */
	private static RegressionMetrics evaluateFile(String filePath) throws Exception {
		FileReader fileReader = new FileReader(filePath);
		CSVReader csvReader = new CSVReaderBuilder(fileReader).withSkipLines(1).build();
		List<String[]> allData = csvReader.readAll();
		csvReader.close();

		float sumSquaredError = 0.0f;
		float sumAbsoluteError = 0.0f;
		float sumAbsoluteRelativeError = 0.0f;
		float epsilon = 1.0e-3f;
		int count = 0;

		// Accumulate error terms across all samples.
		for (String[] row : allData) {
			float yTrue = Float.parseFloat(row[0]);
			float yPredicted = Float.parseFloat(row[1]);
			float error = yTrue - yPredicted;
			float absError = Math.abs(error);

			sumSquaredError += error * error;
			sumAbsoluteError += absError;
			sumAbsoluteRelativeError += absError / (Math.abs(yTrue) + epsilon);
			count++;
		}

		// Convert cumulative sums to means.
		float mse = sumSquaredError / count;
		float mae = sumAbsoluteError / count;
		float mare = sumAbsoluteRelativeError / count;

		return new RegressionMetrics(mse, mae, mare);
	}

	/**
	 * Prints the metric block for a given model file.
	 *
	 * @param fileName model CSV filename
	 * @param metrics computed metrics for that file
	 */
	private static void printMetrics(String fileName, RegressionMetrics metrics) {
		System.out.println("for " + fileName);
		System.out.println("\tMSE =" + metrics.mse);
		System.out.println("\tMAE =" + metrics.mae);
		System.out.println("\tMARE =" + metrics.mare);
	}

	/**
	 * Finds the index of the smallest value in an array.
	 *
	 * @param values array to scan
	 * @return index of the minimum value
	 */
	private static int indexOfMinimum(float[] values) {
		int minIndex = 0;
		for (int i = 1; i < values.length; i++) {
			if (values[i] < values[minIndex]) {
				minIndex = i;
			}
		}
		return minIndex;
	}

    public static void main( String[] args )
    {
		// The three validation result files to evaluate.
		String[] modelFiles = {"model_1.csv", "model_2.csv", "model_3.csv"};
		RegressionMetrics[] results = new RegressionMetrics[modelFiles.length];

		try {
			// Compute and print metrics for each model file.
			for (int i = 0; i < modelFiles.length; i++) {
				results[i] = evaluateFile(modelFiles[i]);
				printMetrics(modelFiles[i], results[i]);
			}
		}
		catch (Exception e) {
			System.out.println("Error reading the CSV file");
			return;
		}

		float[] mseValues = new float[modelFiles.length];
		float[] maeValues = new float[modelFiles.length];
		float[] mareValues = new float[modelFiles.length];

		// Split each metric into a separate array to rank models per metric.
		for (int i = 0; i < modelFiles.length; i++) {
			mseValues[i] = results[i].mse;
			maeValues[i] = results[i].mae;
			mareValues[i] = results[i].mare;
		}

		// Lower values indicate better performance for all three metrics.
		int bestMseIndex = indexOfMinimum(mseValues);
		int bestMaeIndex = indexOfMinimum(maeValues);
		int bestMareIndex = indexOfMinimum(mareValues);

		System.out.println("According to MSE, The best model is " + modelFiles[bestMseIndex]);
		System.out.println("According to MAE, The best model is " + modelFiles[bestMaeIndex]);
		System.out.println("According to MARE, The best model is " + modelFiles[bestMareIndex]);
    }
}
