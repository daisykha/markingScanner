package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.TransferMode;

public class uiController {
	Bubble bubbleDetection;
	@FXML
	private ImageView imgView;
	@FXML
	private TextField answerSheet;
	
    @FXML
    void imageDragOver(DragEvent de) {
      Dragboard board = de.getDragboard();
      if (board.hasFiles()) {
        de.acceptTransferModes(TransferMode.ANY);
      }
    }
    
    @FXML
    void checkAnswer() {
    	String sheet = answerSheet.getText();
    	String[] splitString = sheet.split(",");
    	List<Integer> CorrectAnswer = new ArrayList<>();
    	
    	for(int i=0; i< splitString.length; i++) CorrectAnswer.add(Integer.parseInt(splitString[i]));
    	System.out.println(CorrectAnswer);
    	bubbleDetection.CheckAndOverlay(CorrectAnswer);
    	imgView.setImage(bubbleDetection.ToFxImage());
    	
    }

    @FXML
    void imageDropped(DragEvent de) throws InterruptedException {
      try {
        Dragboard board = de.getDragboard();
        List<File> phil = board.getFiles();
        FileInputStream fis;
          fis = new FileInputStream(phil.get(0));
        Image image = new Image(fis);
        imgView.setImage(image);
        
        System.out.println(phil.get(0).getAbsolutePath());
        bubbleDetection = new Bubble(phil.get(0).getAbsolutePath());
        imgView.setImage(bubbleDetection.ToFxImage());
        
      
        
        
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
	
}
