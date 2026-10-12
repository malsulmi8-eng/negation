package com.example;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class EvaluateNegation {
    public static void main(String[] args) {
        // read using buffered reader the file
        // results
        try {
            BufferedReader reader = new BufferedReader(
                    new FileReader(
                            "abstracts_one_Claudeout_fixed.txt"));

            String line;
            int total = 0;
            int mismatches = 0;
            int correctNegation = 0;
            int incorrectNegation = 0;
            int numberOfNegations = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\[");
                int actualLabel = Integer.parseInt(parts[0].split(",")[1].trim());
                int predictedLabel = Integer.parseInt(parts[1].split(",")[0].trim());

                total++;
                if (predictedLabel == 1 && actualLabel == 1) {
                    correctNegation++;
                } else if (predictedLabel == 1 && actualLabel == 0) {
                    incorrectNegation++;
                    System.out.println(line);
                } else if (predictedLabel == 0 && actualLabel == 1) {
                    System.out.println("False negative found in line: " + line);
                }
                if (actualLabel != predictedLabel) {
                    System.out.println("Mismatch found in line: " + line);
                    mismatches++;
                }
                if (actualLabel == 1) {
                    numberOfNegations++;
                }

                // For scope evaluation, you can implement a similar comparison if you have the
                // actual scope.
            }
            System.out.println("Total lines evaluated: " + total);
            System.out.println("Mismatches found: " + mismatches);
            System.out.println("Accuracy: " + (double) (total - mismatches) / total * 100 + "%");
            // print precision and recall for negation detection
            double precision = (double) correctNegation / (correctNegation + incorrectNegation);
            double recall = (double) correctNegation / numberOfNegations;
            double f1 = 2 * (precision * recall) / (precision + recall);
            System.out.println("Precision: " + precision * 100 + "%");
            System.out.println("Recall: " + recall * 100 + "%");
            System.out.println("F1 score: " + f1 * 100 + "%");
        } catch (IOException | NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
