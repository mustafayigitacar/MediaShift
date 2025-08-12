package com.ffmpeg.gui;

import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Basit editable spinner yardımcı sınıfı
 */
public class SimpleEditableSpinner {
    
    /**
     * Integer spinnerı editable yapar
     */
    public static void makeEditable(Spinner<Integer> spinner, int min, int max, int defaultValue) {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, defaultValue);
        spinner.setValueFactory(valueFactory);
        spinner.setEditable(true);
        
        // Text fieldı al
        TextField editor = spinner.getEditor();
        
        // Enter tuşuna basıldığında değeri güncelle
        editor.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                try {
                    String text = editor.getText();
                    int value = Integer.parseInt(text);
                    if (value >= min && value <= max) {
                        spinner.getValueFactory().setValue(value);
                    } else {
                        spinner.getValueFactory().setValue(defaultValue);
                        editor.setText(String.valueOf(defaultValue));
                    }
                } catch (NumberFormatException e) {
                    spinner.getValueFactory().setValue(defaultValue);
                    editor.setText(String.valueOf(defaultValue));
                }
            }
        });
        
        // Focus kaybedildiğinde değeri güncelle
        editor.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // Focus kaybedildiğinde
                try {
                    String text = editor.getText();
                    int value = Integer.parseInt(text);
                    if (value >= min && value <= max) {
                        spinner.getValueFactory().setValue(value);
                    } else {
                        spinner.getValueFactory().setValue(defaultValue);
                        editor.setText(String.valueOf(defaultValue));
                    }
                } catch (NumberFormatException e) {
                    spinner.getValueFactory().setValue(defaultValue);
                    editor.setText(String.valueOf(defaultValue));
                }
            }
        });
    }
    
    /**
     * Double spinnerı editable yapar
     */
    public static void makeEditable(Spinner<Double> spinner, double min, double max, double defaultValue, double step) {
        SpinnerValueFactory<Double> valueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, defaultValue, step);
        spinner.setValueFactory(valueFactory);
        spinner.setEditable(true);
        
        // Text fieldı al
        TextField editor = spinner.getEditor();
        
        // Enter tuşuna basıldığında değeri güncelle
        editor.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                try {
                    String text = editor.getText();
                    double value = Double.parseDouble(text);
                    if (value >= min && value <= max) {
                        spinner.getValueFactory().setValue(value);
                    } else {
                        spinner.getValueFactory().setValue(defaultValue);
                        editor.setText(String.valueOf(defaultValue));
                    }
                } catch (NumberFormatException e) {
                    spinner.getValueFactory().setValue(defaultValue);
                    editor.setText(String.valueOf(defaultValue));
                }
            }
        });
        
        // Focus kaybedildiğinde değeri güncelle
        editor.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // Focus kaybedildiğinde
                try {
                    String text = editor.getText();
                    double value = Double.parseDouble(text);
                    if (value >= min && value <= max) {
                        spinner.getValueFactory().setValue(value);
                    } else {
                        spinner.getValueFactory().setValue(defaultValue);
                        editor.setText(String.valueOf(defaultValue));
                    }
                } catch (NumberFormatException e) {
                    spinner.getValueFactory().setValue(defaultValue);
                    editor.setText(String.valueOf(defaultValue));
                }
            }
        });
    }
} 