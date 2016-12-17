package jme3tools.navmesh;

import jme3tools.navmesh.Path.Waypoint;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * NavigationPath is a collection of waypoints that define a movement path for
 * an Actor. This object is ownded by an Actor and filled by
 * NavigationMesh::BuildNavigationPath().
 * 
 * Portions Copyright (C) Greg Snook, 2000
 * 
 * @author TR
 * 
 */
public class Path implements Iterable<Waypoint> {

    public class Waypoint {

        private Vector3f position;
        private Cell cell;

        /**
         * The cell which owns the waypoint
         */
        public Cell getCell() {
            return cell;
        }

        public void setCell(Cell cell) {
            this.cell = cell;
        }

        /**
         * 3D position of waypoint
         */
        public Vector3f getPosition() {
            return position;
        }

        public void setPosition(Vector3f position) {
            this.position = position;
        }

        @Override
        public String toString() {
            return "Waypoint[position=" + position.x + ", " + position.z + " cell:"
                    + cell + "]";
        }
    }

    private NavMesh owner;
    private Waypoint start = new Waypoint();
    private Waypoint end = new Waypoint();
    private ArrayList<Waypoint> waypointList = new ArrayList<Waypoint>();

    /**
     * Sets up a new path from StartPoint to EndPoint. It adds the StartPoint as
     * the first waypoint in the list and waits for further calls to AddWayPoint
     * and EndPath to complete the list
     * 
     * @param parent
     * @param startPoint
     * @param startCell
     * @param endPoint
     * @param endCell
     */
    public void initialize(NavMesh parent,
                           Vector3f startPoint, Cell startCell,
                           Vector3f endPoint, Cell endCell) {

        waypointList.clear();

        this.owner = parent;

        start.setPosition(startPoint);
        start.setCell(startCell);

        end.setPosition(endPoint);
        end.setCell(endCell);

        // setup the waypoint list with our start and end points
        waypointList.add(start);
    }

    public void clear() {
        waypointList.clear();
    }

    public int size(){
        return waypointList.size();
    }

    public Iterator<Waypoint> iterator() {
        return waypointList.iterator();
    }

    /**
     * Adds a new waypoint to the end of the list
     */
    public void addWaypoint(Vector3f point, Cell cell) {
        Waypoint newPoint = new Waypoint();
        newPoint.setPosition(point);
        newPoint.setCell(cell);
        waypointList.add(newPoint);
    }

    /**
     * Caps the end of the waypoint list by adding our final destination point.
     */
    void finishPath() {
        // cap the waypoint path with the last endpoint
        waypointList.add(end);
    }

    public NavMesh getOwner() {
        return owner;
    }

    public Waypoint getStart() {
        return start;
    }

    public Waypoint getEnd() {
        return end;
    }

    public Waypoint getFirst(){
        return waypointList.get(0);
    }

    public Waypoint getLast(){
        return waypointList.get(waypointList.size()-1);
    }

    public ArrayList<Waypoint> getWaypoints() {
        return waypointList;
    }

    /**
     * Find the furthest visible waypoint from the VantagePoint provided. This
     * is used to smooth out irregular paths.
     * 
     * @param vantagePoint
     * @return
     */
    public Waypoint getFurthestVisibleWayPoint(Waypoint vantagePoint) {
        // see if we are already talking about the last waypoint
        if (vantagePoint == getLast()) {
            return vantagePoint;
        }

        int i = waypointList.indexOf(vantagePoint);
        if (i < 0) {
            // The given waypoint does not belong to this path.
            return vantagePoint;
        }

        Waypoint testPoint = waypointList.get(++i);
        if (testPoint == getLast()) {
            System.out.println(" WAY IND was last");
            return testPoint;
        }

        Waypoint visibleWaypoint = testPoint;
        while (testPoint != getLast()) {
            if (!owner.isInLineOfSight(vantagePoint.cell, vantagePoint.position,
                    testPoint.position)) {
//		System.out.println(" WAY IND was:" + i);
                return visibleWaypoint;
            }
            visibleWaypoint = testPoint;
            testPoint = waypointList.get(++i);
        }
        return testPoint;
    }
}
