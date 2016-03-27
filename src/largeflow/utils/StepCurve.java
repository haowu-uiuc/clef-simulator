package largeflow.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class StepCurve {
    private List<Tuple<Double, Integer>> jumpPoints;
    private Double endTime;
    
    public StepCurve(Double endTime) {
        this.endTime = endTime;
        jumpPoints = new LinkedList<>();
        jumpPoints.add(new Tuple<Double, Integer>(0., 0));
    }
    
    /**
     * 
     * @param startPoint
     *            : start point of a burst
     * @param endPointTime
     *            : time of the end point of a burst, value of end point is
     *            always zero!
     */
    public void addCurve(Tuple<Double, Integer> startPoint,
            Double endPointTime) {
        
        Double finalEndPointTime = endPointTime;
        
        if (finalEndPointTime <= startPoint.first) {
            return;
        }
        
        if (startPoint.first >= endTime) {
            return;
        }
        
        if (finalEndPointTime > endTime) {
            finalEndPointTime = endTime;
        }
        
        // find place to insert startPoint and endPoint
        Tuple<Double, Integer> prePoint = null;
        int i = -1;
        int startIndex = -1;
        int endIndex = -1;
        
        for(Tuple<Double, Integer> curPoint : jumpPoints) {
            i++;
            
            if (startIndex < 0) {
                // find the place of start point
                if (curPoint.first >= startPoint.first
                        && (prePoint == null || prePoint.first < startPoint.first)) {
                    startIndex = i;
                }
            }
            
            if (startIndex >= 0 && endIndex < 0){
                // find the place of end point
                if (curPoint.first >= finalEndPointTime
                        && (prePoint == null || prePoint.first < finalEndPointTime)) {
                    endIndex = i;
                }
            }
            
            if (startIndex >= 0 && endIndex >= 0) {
                break;
            }
            
            prePoint = curPoint;
        }
                
        if (startIndex < 0) {
            // didnt find start and end points between existing points,
            // append the startPoint and endPoint at the end
            startIndex = jumpPoints.size();
            endIndex = jumpPoints.size();
        } else if (endIndex < 0) {
            // didnt find an end points between existing points,
            // append the endPoint at the end
            endIndex = jumpPoints.size();
        }
        
        endIndex ++; // because we insert startPoint first, so endIndex++ is necessary
        
        
        // insert the startPoint
        if (startIndex == 0) {
            jumpPoints.add(startIndex, new Tuple<Double, Integer>(startPoint.first, 0));
        } else {
            int preStartValue = jumpPoints.get(startIndex - 1).second;
            jumpPoints.add(startIndex, new Tuple<Double, Integer>(startPoint.first, preStartValue));
        }
        
        // insert the endPoint
        int preEndValue = jumpPoints.get(endIndex - 1).second;
        jumpPoints.add(endIndex, new Tuple<Double, Integer>(finalEndPointTime, preEndValue));
        
        // de-duplication: startIndex and startIndex+1 could be identical
        // endIndex and endIndex+1 could be identical
        if (jumpPoints.get(startIndex).first.equals(jumpPoints.get(startIndex+1).first)) {
            jumpPoints.remove(startIndex);
            endIndex --;
        }
        
        if (endIndex < jumpPoints.size() - 1
                && jumpPoints.get(endIndex).first.equals(jumpPoints.get(endIndex+1).first)) {
            jumpPoints.remove(endIndex);
        }
        
        
        // increase the value for all points between [startIndex, endIndex)
        for (int j = startIndex; j < endIndex; j++) {
            jumpPoints.get(j).second += startPoint.second;
        }
        
        // deduplicate again
        prePoint = null;
        List<Tuple<Double, Integer>> pointsToRemove = new ArrayList<>();
        for (Tuple<Double, Integer> curPoint : jumpPoints) {
            if (prePoint != null && prePoint.second.equals(curPoint.second)) {
                pointsToRemove.add(curPoint);
            }
            prePoint = curPoint;
        }
        for (Tuple<Double, Integer> curPoint : pointsToRemove) {
            jumpPoints.remove(curPoint);
        }
        
        
    }
    
    public Double endTime() {
        return endTime;
    }
    
    public List<Tuple<Double, Integer>> getJumpPoints() {
        return jumpPoints;
    }
    
    @Override
    public String toString() {
        return jumpPoints.toString();
    }
    
}
