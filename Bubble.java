package application;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javafx.scene.image.Image;
import javafx.util.Pair;

public class Bubble {
	private Mat image,edgesImage, lastResult;
	List<Integer> Answer;
	List<List<MatOfPoint>> sortedQuestionContours;
	
	public Image ToFxImage() {
		MatOfByte buffer = new MatOfByte();
		Imgcodecs.imencode(".png", lastResult, buffer);
		return new Image(new ByteArrayInputStream(buffer.toArray())); 
	}
	
	public Bubble(String absolutePath) {
		image = Imgcodecs.imread(absolutePath);
		
		//Imgcodecs.imwrite("F:/abc.jpg", image);
		System.out.println("Loaded image to Mat opencv");
		PreProcess(); 
		System.out.println("Apply Canny edges");
		Process();
	}
	
	
	private void FourPointsTransform(MatOfPoint2f contour, Mat output) {
		List<Point> points = contour.toList();
		
		Mat src = new MatOfPoint2f(points.get(0),
								   points.get(3),
								   points.get(2),
								   points.get(1));
		
		Mat dst = new MatOfPoint2f(new Point(0,0),
								   new Point(image.width()-1,0),
								   new Point(image.width()-1, image.height() -1),
								   new Point(0, image.height()-1));
		
		Mat transform = Imgproc.getPerspectiveTransform(src, dst);
		Imgproc.warpPerspective(image, output, transform, output.size());
	}
	
	
	public void Process() {
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(edgesImage, contours, new Mat(), Imgproc.RETR_TREE,  Imgproc.CHAIN_APPROX_SIMPLE);
		contours.sort((p1, p2) -> {
			double p1Area = Imgproc.contourArea(p1),
				   p2Area = Imgproc.contourArea(p2);
			if (p1Area == p2Area) return 0;
			return (p1Area> p2Area) ? -1 : 1;
		});
		
		System.out.println(contours.size());
		
		MatOfPoint2f BiggestContourHasFourPoints = new MatOfPoint2f();
		for (MatOfPoint contour : contours) {
			MatOfPoint2f contourConverted = new MatOfPoint2f(contour.toArray());
			double peri = Imgproc.arcLength(contourConverted, true);
			MatOfPoint2f approxCurve = new MatOfPoint2f();
			Imgproc.approxPolyDP(contourConverted, approxCurve, 0.02 * peri, true);
			if (approxCurve.toList().size() == 4) {
				System.out.println("FOUND contour has 4 points");
				BiggestContourHasFourPoints = approxCurve;
				break;
			}
		}
		
		Mat TopDownTransform = new Mat();
		FourPointsTransform(BiggestContourHasFourPoints, TopDownTransform);
		
		FindBubble(TopDownTransform);
		
	}
	
	private void SortBubbleTopDown(List<MatOfPoint> contours) {
		contours.sort((p1, p2) -> {
			Rect rect1 = Imgproc.boundingRect(p1),
				 rect2 = Imgproc.boundingRect(p2);
			
			if (rect1.y == rect2.y) return 0;
			return (rect1.y > rect2.y)?1:-1;
		});
	}
	
	private void SortBubbleLeftToRight(List<MatOfPoint> contours) {
		contours.sort((p1, p2) -> {
			Rect rect1 = Imgproc.boundingRect(p1),
				 rect2 = Imgproc.boundingRect(p2);
			
			if (rect1.x == rect2.x) return 0;
			return (rect1.x > rect2.x)?1:-1;
		});	
	}
	
	
	private void FindBubble(Mat src) {
		Mat dst = new Mat();
		Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGR2GRAY);
		Imgproc.threshold(dst, dst, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);
		
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		List<MatOfPoint> questionContours = new ArrayList<>();
		
		Imgproc.findContours(dst, contours, new Mat(), Imgproc.RETR_EXTERNAL,  Imgproc.CHAIN_APPROX_SIMPLE); //RETR_EXTERNAL skip overlap contour
		MatOfPoint cnts = contours.get(0);
		
		for(MatOfPoint contour : contours) {
			Rect rect = Imgproc.boundingRect(contour);
			
			double AspectRatio = (double)rect.width / (double)rect.height;
			
			if ((rect.width >= 50) && (rect.height >= 50) && (AspectRatio >= 0.9) && (AspectRatio <= 1.1)){
				questionContours.add(contour);
			}
		}
		
		SortBubbleTopDown(questionContours);
		
		sortedQuestionContours = new ArrayList<>(); 
		List<Integer> Answer = new ArrayList<>();
		
		for(int i=0; i< questionContours.size(); i+= 5) {
			List<MatOfPoint> SplitFiveBubbles = new ArrayList<>();
			SplitFiveBubbles.add(questionContours.get(i));
			SplitFiveBubbles.add(questionContours.get(i+1));
			SplitFiveBubbles.add(questionContours.get(i+2));
			SplitFiveBubbles.add(questionContours.get(i+3));
			SplitFiveBubbles.add(questionContours.get(i+4));
			
			SortBubbleLeftToRight(SplitFiveBubbles);
			sortedQuestionContours.add(SplitFiveBubbles);
			
			double countNonZeroPixels = 0;
			Answer.add(0);
			for(int j=0; j<5; j++) {
				Mat mask = Mat.zeros(dst.size(), CvType.CV_8UC1);
				Mat exportFromMask = Mat.zeros(dst.size(), CvType.CV_8UC1);
				
				Imgproc.drawContours(mask, SplitFiveBubbles, j, new Scalar(255, 0, 0), -1);
				Core.bitwise_and(dst, mask, exportFromMask);
				double countFromMask = Core.countNonZero(exportFromMask);
				if (countFromMask > countNonZeroPixels) {
					countNonZeroPixels = countFromMask;
					Answer.set(Answer.size()-1, j);
				}
			}	
		}
		
		lastResult = src;
		
		for(int i=0; i< Answer.size(); i++) 
			Imgproc.drawContours(lastResult, sortedQuestionContours.get(i), Answer.get(i), new Scalar(0, 255, 255), 5);
		
		this.Answer = Answer;
		
	} 
	
	public List<Integer> getAnswer(){
		return Answer;
	}
	
	public void CheckAndOverlay(List<Integer> CorrectAnswer) {
		for(int i=0; i< CorrectAnswer.size(); i++) {
			if (CorrectAnswer.get(i) == Answer.get(i)) {
				Imgproc.drawContours(lastResult, sortedQuestionContours.get(i), Answer.get(i), new Scalar(0, 255, 0), 5);
			}else {
				Imgproc.drawContours(lastResult, sortedQuestionContours.get(i), Answer.get(i), new Scalar(0, 0, 255), 5);
			}
		}
		
	}
	public void PreProcess() {
		edgesImage = new Mat();
		
		Imgproc.GaussianBlur(image, edgesImage, new Size(5, 5), 1, 1);
		Imgproc.cvtColor(edgesImage, edgesImage, Imgproc.COLOR_BGR2GRAY);
		Imgproc.Canny(edgesImage, edgesImage, 75, 200);
		
	}

}
