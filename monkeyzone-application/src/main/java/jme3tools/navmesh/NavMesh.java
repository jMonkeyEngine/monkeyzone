package jme3tools.navmesh;

import java.io.IOException;
import java.util.ArrayList;

import jme3tools.navmesh.Cell.ClassifyResult;
import jme3tools.navmesh.Cell.PathResult;
import jme3tools.navmesh.Line2D.LineIntersect;
import jme3tools.navmesh.Path.Waypoint;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * A NavigationMesh is a collection of NavigationCells used to control object
 * movement while also providing path finding line-of-sight testing. It serves
 * as a parent to all the Actor objects which exist upon it.
 * 
 * Portions Copyright (C) Greg Snook, 2000
 * 
 * @author TR
 * 
 */
public class NavMesh implements Savable {

    /**
     * the cells that make up this mesh
     */
    private ArrayList<Cell> cellList = new ArrayList<Cell>();

    /**
     * path finding data...
     */
    private volatile int sessionID = 0;
    private volatile Heap heap = new Heap();

    public void clear() {
        cellList.clear();
    }

    /**
     * Add a new cell, defined by the three vertices in clockwise order, to this
     * mesh.
     *
     * @param pointA
     * @param PointB
     * @param PointC
     */
    public void addCell(Vector3f pointA, Vector3f PointB, Vector3f PointC) {
        Cell newCell = new Cell();
        newCell.initialize(pointA.clone(), PointB.clone(), PointC.clone());
        cellList.add(newCell);
    }

    /**
     * Does noting at this point. Stubbed for future use in animating the mesh
     * @param elapsedTime
     */
    void Update(float elapsedTime) {
    }

    public int getNumCells() {
        return cellList.size();
    }

    public Cell getCell(int index) {
        return (cellList.get(index));
    }

    /**
     * Force a point to be inside the cell
     */
    public Vector3f snapPointToCell(Cell cell, Vector3f point) {
        if (!cell.contains(point))
            cell.forcePointToCellColumn(point);
        
        cell.computeHeightOnCell(point);
        return point;
    }

    /**
     * Force a point to be inside the nearest cell on the mesh
     */
    Vector3f snapPointToMesh(Vector3f point) {
        return snapPointToCell(findClosestCell(point), point);
    }

    /**
     * Find the closest cell on the mesh to the given point
     */
    public Cell findClosestCell(Vector3f point) {
        float closestDistance = 3.4E+38f;
        float closestHeight = 3.4E+38f;
        boolean foundHomeCell = false;
        float thisDistance;
        Cell closestCell = null;

        for (Cell cell : cellList) {
            if (cell.contains(point)) {
                thisDistance = Math.abs(cell.getHeightOnCell(point) - point.y);

                if (foundHomeCell) {
                    if (thisDistance < closestHeight) {
                        closestCell = cell;
                        closestHeight = thisDistance;
                    }
                } else {
                    closestCell = cell;
                    closestHeight = thisDistance;
                    foundHomeCell = true;
                }
            }

            if (!foundHomeCell) {
                Vector2f start = new Vector2f(cell.getCenter().x, cell.getCenter().z);
                Vector2f end = new Vector2f(point.x, point.z);
                Line2D motionPath = new Line2D(start, end);

                ClassifyResult Result = cell.classifyPathToCell(motionPath);

                if (Result.result == Cell.PathResult.ExitingCell) {
                    Vector3f ClosestPoint3D = new Vector3f(
                            Result.intersection.x, 0.0f, Result.intersection.y);
                    cell.computeHeightOnCell(ClosestPoint3D);

                    ClosestPoint3D = ClosestPoint3D.subtract(point);

                    thisDistance = ClosestPoint3D.length();

                    if (thisDistance < closestDistance) {
                        closestDistance = thisDistance;
                        closestCell = cell;
                    }
                }
            }
        }

        return closestCell;
    }

    /**
     * Build a navigation path using the provided points and the A* method
     */
    public boolean buildNavigationPath(Path navPath,
            Cell startCell, Vector3f startPos,
            Cell endCell, Vector3f endPos,
            float entityRadius) {
        
        // Increment our path finding session ID
        // This Identifies each pathfinding session
        // so we do not need to clear out old data
        // in the cells from previous sessions.
        sessionID++;

        // load our data into the Heap object
        // to prepare it for use.
        heap.initialize(sessionID, startPos);

        // We are doing a reverse search, from EndCell to StartCell.
        // Push our EndCell onto the Heap at the first cell to be processed
        endCell.queryForPath(heap, null, 0.0f);

        // process the heap until empty, or a path is found
        boolean foundPath = false;
        while (heap.isNotEmpty() && !foundPath) {

            // pop the top cell (the open cell with the lowest cost) off the
            // Heap
            Node currentNode = heap.getTop();

            // if this cell is our StartCell, we are done
            if (currentNode.cell.equals(startCell)) {
                foundPath = true;
            } else {
                // Process the Cell, Adding it's neighbors to the Heap as needed
                currentNode.cell.processCell(heap);
            }
        }

        Vector2f intersectionPoint = new Vector2f();

        // if we found a path, build a waypoint list
        // out of the cells on the path
        if (!foundPath)
            return false;

        // Setup the Path object, clearing out any old data
        navPath.initialize(this, startPos, startCell, endPos, endCell);

        Vector3f lastWayPoint = startPos;

        // Step through each cell linked by our A* algorythm
        // from StartCell to EndCell
        Cell currentCell = startCell;
        while (currentCell != null && currentCell != endCell) {
            // add the link point of the cell as a way point (the exit
            // wall's center)
            int linkWall = currentCell.getArrivalWall();
            Vector3f newWayPoint = currentCell.getWallMidpoint(linkWall).clone();

            Line2D wall  = currentCell.getWall(linkWall);
            float length = wall.length();
            float distBlend = entityRadius / length;

            Line2D lineToGoal = new Line2D(new Vector2f(lastWayPoint.x, lastWayPoint.z),
                                           new Vector2f(endPos.x, endPos.z));
            LineIntersect result = lineToGoal.intersect(wall, intersectionPoint);
            switch (result){
                case SegmentsIntersect:
                    float d1 = wall.getPointA().distance(intersectionPoint);
                    float d2 = wall.getPointB().distance(intersectionPoint);
                    if (d1 > entityRadius && d2 > entityRadius){
                        // we can fit through the wall if we go
                        // directly to the goal.
                        newWayPoint = new Vector3f(intersectionPoint.x, 0, intersectionPoint.y);
                    }else{
                        // cannot fit directly.
                        // try to find point where we can
                        if (d1 < d2){
                            intersectionPoint.interpolateLocal(wall.getPointA(), wall.getPointB(), distBlend);
                            newWayPoint = new Vector3f(intersectionPoint.x, 0, intersectionPoint.y);
                        }else{
                            intersectionPoint.interpolateLocal(wall.getPointB(), wall.getPointA(), distBlend);
                            newWayPoint = new Vector3f(intersectionPoint.x, 0, intersectionPoint.y);
                        }
                    }
                    currentCell.computeHeightOnCell(newWayPoint);
                    break;
                case LinesIntersect:
                case ABisectsB:
                case BBisectsA:
                    Vector2f lastPt2d  = new Vector2f(lastWayPoint.x, lastWayPoint.z);
                    Vector2f endPos2d  = new Vector2f(endPos.x, endPos.z);

                    Vector2f normalEnd = endPos2d.subtract(lastPt2d).normalizeLocal();
                    Vector2f normalA   = wall.getPointA().subtract(lastPt2d).normalizeLocal();
                    Vector2f normalB   = wall.getPointB().subtract(lastPt2d).normalizeLocal();
                    if (normalA.dot(normalEnd) < normalB.dot(normalEnd)){
                        // choose point b
                        intersectionPoint.interpolateLocal(wall.getPointB(), wall.getPointA(), distBlend);
                        newWayPoint = new Vector3f(intersectionPoint.x, 0, intersectionPoint.y);
                    }else{
                        // choose point a
                        intersectionPoint.interpolateLocal(wall.getPointA(), wall.getPointB(), distBlend);
                        newWayPoint = new Vector3f(intersectionPoint.x, 0, intersectionPoint.y);
                    }
                    currentCell.computeHeightOnCell(newWayPoint);

                    break;
                case CoLinear:
                case Parallel:
                    break;
            }

//                newWayPoint = snapPointToCell(currentCell, newWayPoint);
            lastWayPoint = newWayPoint.clone();

            navPath.addWaypoint(newWayPoint, currentCell);

            // get the next cell
            currentCell = currentCell.getLink(linkWall);
        }

        // cap the end of the path.
        navPath.finishPath();

        // further: optimize the path
        List<Waypoint> newPath = new ArrayList<Waypoint>();
        Waypoint curWayPoint = navPath.getFirst();
        newPath.add(curWayPoint);
        while (curWayPoint != navPath.getLast()){
            curWayPoint = navPath.getFurthestVisibleWayPoint(curWayPoint);
            newPath.add(curWayPoint);
        }

        navPath.initialize(this, startPos, startCell, endPos, endCell);
        for (Waypoint newWayPoint : newPath){
            navPath.addWaypoint(newWayPoint.getPosition(), newWayPoint.getCell());
        }
        navPath.finishPath();

        return true;
    }

    /**
     * Resolve a movement vector on the mesh
     *
     * @param startPos
     * @param startCell
     * @param endPos
     * @return
     */
    public Cell resolveMotionOnMesh(Vector3f startPos, Cell startCell, Vector3f endPos, Vector3f modifiedEndPos) {
        int i = 0;
        // create a 2D motion path from our Start and End positions, tossing out
        // their Y values to project them
        // down to the XZ plane.
        Line2D motionLine = new Line2D(new Vector2f(startPos.x, startPos.z),
                new Vector2f(endPos.x, endPos.z));

        // these three will hold the results of our tests against the cell walls
        ClassifyResult result = null;

        // TestCell is the cell we are currently examining.
        Cell currentCell = startCell;

        do {
            i++;
            // use NavigationCell to determine how our path and cell interact
            // if(TestCell.IsPointInCellCollumn(MotionPath.EndPointA()))
            // System.out.println("Start is in cell");
            // else
            // System.out.println("Start is NOT in cell");
            // if(TestCell.IsPointInCellCollumn(MotionPath.EndPointB()))
            // System.out.println("End is in cell");
            // else
            // System.out.println("End is NOT in cell");
            result = currentCell.classifyPathToCell(motionLine);

            // if exiting the cell...
            if (result.result == PathResult.ExitingCell) {
                // Set if we are moving to an adjacent cell or we have hit a
                // solid (unlinked) edge
                if (result.cell != null) {
                    // moving on. Set our motion origin to the point of
                    // intersection with this cell
                    // and continue, using the new cell as our test cell.
                    motionLine.setPointA(result.intersection);
                    currentCell = result.cell;
                } else {
                    // we have hit a solid wall. Resolve the collision and
                    // correct our path.
                    motionLine.setPointA(result.intersection);
                    currentCell.projectPathOnCellWall(result.side, motionLine);

                    // add some friction to the new MotionPath since we are
                    // scraping against a wall.
                    // we do this by reducing the magnatude of our motion by 10%
                    Vector2f Direction = motionLine.getPointB().subtract(
                            motionLine.getPointA()).mult(0.9f);
                    // Direction.mult(0.9f);
                    motionLine.setPointB(motionLine.getPointA().add(
                            Direction));
                }
            } else if (result.result == Cell.PathResult.NoRelationship) {
                // Although theoretically we should never encounter this case,
                // we do sometimes find ourselves standing directly on a vertex
                // of the cell.
                // This can be viewed by some routines as being outside the
                // cell.
                // To accomodate this rare case, we can force our starting point
                // to be within
                // the current cell by nudging it back so we may continue.
                Vector2f NewOrigin = motionLine.getPointA();
                // NewOrigin.x -= 0.01f;
                currentCell.forcePointToCellColumn(NewOrigin);
                motionLine.setPointA(NewOrigin);
            }
        }//
        // Keep testing until we find our ending cell or stop moving due to
        // friction
        //
        while ((result.result != Cell.PathResult.EndingCell)
                && (motionLine.getPointA().x != motionLine.getPointB().x && motionLine.getPointA().y != motionLine.getPointB().y) && i < 5000);
        //
        if (i >= 5000) {
            System.out.println("Loop detected in ResolveMotionOnMesh");
        }
        // we now have our new host cell

        // Update the new control point position,
        // solving for Y using the Plane member of the NavigationCell
        modifiedEndPos.x = motionLine.getPointB().x;
        modifiedEndPos.y = 0.0f;
        modifiedEndPos.z = motionLine.getPointB().y;
        currentCell.computeHeightOnCell(modifiedEndPos);

        return currentCell;
    }

    /**
     * Test to see if two points on the mesh can view each other
     * FIXME: EndCell is the last visible cell?
     *
     * @param StartCell
     * @param StartPos
     * @param EndPos
     * @return
     */
    boolean isInLineOfSight(Cell StartCell, Vector3f StartPos, Vector3f EndPos) {
        Line2D MotionPath = new Line2D(new Vector2f(StartPos.x, StartPos.z),
                new Vector2f(EndPos.x, EndPos.z));

        Cell testCell = StartCell;
        Cell.ClassifyResult result = testCell.classifyPathToCell(MotionPath);
        
        while (result.result == Cell.PathResult.ExitingCell) {
            if (result.cell == null)// hit a wall, so the point is not visible
            {
                return false;
            }
            result = result.cell.classifyPathToCell(MotionPath);

        }

        return (result.result == Cell.PathResult.EndingCell);
    }

    /**
     * Link all the cells that are in our pool
     */
    public void linkCells() {
//        for (int i = 0; i < cellList.size(); i++){
//            for (int j = i+1; j < cellList.size(); j++){
//                cellList.get(i).checkAndLink(cellList.get(j));
//            }
//        }
        for (Cell pCellA : cellList) {
            for (Cell pCellB : cellList) {
                if (pCellA != pCellB) {
                    pCellA.checkAndLink(pCellB, 0.001f);
                }
            }
        }
    }

    private void addFace(Vector3f vertA, Vector3f vertB, Vector3f vertC) {
        // some art programs can create linear polygons which have two or more
        // identical vertices. This creates a poly with no surface area,
        // which will wreak havok on our navigation mesh algorithms.
        // We only except polygons with unique vertices.
        if ((!vertA.equals(vertB)) && (!vertB.equals(vertC)) && (!vertC.equals(vertA))) {
            addCell(vertA, vertB, vertC);
        }else{
            System.out.println("Warning, Face winding incorrect");
        }
    }

    public void loadFromData(Vector3f[] positions, short[][] indices){
        Plane up = new Plane();
        up.setPlanePoints(Vector3f.UNIT_X, Vector3f.ZERO, Vector3f.UNIT_Z);
        up.getNormal();

        for (int i = 0; i < indices.length/3; i++) {
            Vector3f vertA = positions[indices[i][0]];
            Vector3f vertB = positions[indices[i][1]];
            Vector3f vertC = positions[indices[i][2]];
            
            Plane p = new Plane();
            p.setPlanePoints(vertA, vertB, vertC);
            if (up.pseudoDistance(p.getNormal()) <= 0.0f) {
                System.out.println("Warning, normal of the plane faces downward!!!");
                continue;
            }

            addFace(vertA, vertB, vertC);
        }

        linkCells();
    }

    public void loadFromMesh(Mesh mesh) {
        clear();

        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();
        Vector3f c = new Vector3f();

        Plane up = new Plane();
        up.setPlanePoints(Vector3f.UNIT_X, Vector3f.ZERO, Vector3f.UNIT_Z);
        up.getNormal();

        IndexBuffer ib = mesh.getIndexBuffer();
        FloatBuffer pb = mesh.getFloatBuffer(Type.Position);
        pb.clear();
        for (int i = 0; i < mesh.getTriangleCount()*3; i+=3){
            int i1 = ib.get(i+0);
            int i2 = ib.get(i+1);
            int i3 = ib.get(i+2);
            BufferUtils.populateFromBuffer(a, pb, i1);
            BufferUtils.populateFromBuffer(b, pb, i2);
            BufferUtils.populateFromBuffer(c, pb, i3);

            Plane p = new Plane();
            p.setPlanePoints(a, b, c);
            if (up.pseudoDistance(p.getNormal()) <= 0.0f) {
                System.out.println("Warning, normal of the plane faces downward!!!");
                continue;
            }

            addFace(a, b, c);
        }

        linkCells();
    }

    public void write(JmeExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.writeSavableArrayList(cellList, "cellarray", null);
    }

    @SuppressWarnings("unchecked")
    public void read(JmeImporter e) throws IOException {
        InputCapsule capsule = e.getCapsule(this);
        cellList = (ArrayList<Cell>) capsule.readSavableArrayList("cellarray", new ArrayList<Cell>());
    }
}
