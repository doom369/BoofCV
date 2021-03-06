/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.abst.geo.pose;

import boofcv.abst.geo.EstimateNofPnP;
import boofcv.alg.geo.pose.P3PLineDistance;
import boofcv.alg.geo.pose.PointDistance3;
import boofcv.struct.geo.Point2D3D;
import georegression.fitting.MotionTransformPoint;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts solutions generated by P3PLineDistance into rigid body motions.
 *
 * @author Peter Abeles
 */
public class WrapP3PLineDistance implements EstimateNofPnP {

	// estimates the distance the camera center is from each of the 3 points.
	private P3PLineDistance alg;
	// computes the optimal rigid body motion between the two views given a point cloud
	private MotionTransformPoint<Se3_F64, Point3D_F64> motionFit;

	// location of 3D point given the found distance
	private Point3D_F64 X1 = new Point3D_F64();
	private Point3D_F64 X2 = new Point3D_F64();
	private Point3D_F64 X3 = new Point3D_F64();

	// observations normalized to 1
	private Vector3D_F64 u1 = new Vector3D_F64();
	private Vector3D_F64 u2 = new Vector3D_F64();
	private Vector3D_F64 u3 = new Vector3D_F64();

	// storage for 3D point clouds.
	// World = point in world coordinate system and Camera = camera coordinates sytsem
	private List<Point3D_F64> cloudWorld = new ArrayList<Point3D_F64>();
	private List<Point3D_F64> cloudCamera = new ArrayList<Point3D_F64>();

	public WrapP3PLineDistance(P3PLineDistance alg,
							   MotionTransformPoint<Se3_F64, Point3D_F64> motionFit )
	{
		this.alg = alg;
		this.motionFit = motionFit;

		cloudCamera.add(X1);
		cloudCamera.add(X2);
		cloudCamera.add(X3);
	}

	@Override
	public boolean process(List<Point2D3D> inputs , FastQueue<Se3_F64> solutions ) {
		if( inputs.size() != 3 )
			throw new IllegalArgumentException("Three and only three inputs are required.  Not "+inputs.size());

		solutions.reset();

		Point2D3D P1 = inputs.get(0);
		Point2D3D P2 = inputs.get(1);
		Point2D3D P3 = inputs.get(2);

		// Compute the length of each side in the triangle
		double length12 = P1.location.distance(P2.getLocation());
		double length13 = P1.location.distance(P3.getLocation());
		double length23 = P2.location.distance(P3.getLocation());

		if( !alg.process(P1.observation,P2.observation,P3.observation,length23,length13,length12))
			return false;

		FastQueue<PointDistance3> distances = alg.getSolutions();

		if( distances.size == 0 )
			return false;

		// convert observations into a 3D pointing vector and normalize to one
		u1.set(P1.observation.x,P1.observation.y,1); // homogeneous coordinates
		u2.set(P2.observation.x,P2.observation.y,1);
		u3.set(P3.observation.x,P3.observation.y,1);

		u1.normalize(); u2.normalize(); u3.normalize();

		// set up world point cloud
		cloudWorld.clear();
		cloudWorld.add(P1.location);
		cloudWorld.add(P2.location);
		cloudWorld.add(P3.location);

		for( int i = 0; i < distances.size; i++ ) {
			PointDistance3 pd = distances.get(i);

			X1.set( u1.x*pd.dist1 , u1.y*pd.dist1 , u1.z*pd.dist1 );
			X2.set( u2.x*pd.dist2 , u2.y*pd.dist2 , u2.z*pd.dist2 );
			X3.set( u3.x*pd.dist3 , u3.y*pd.dist3 , u3.z*pd.dist3 );

			if( !motionFit.process(cloudWorld,cloudCamera) )
				continue;

			Se3_F64 found = solutions.grow();
			found.set( motionFit.getTransformSrcToDst() );
		}


		return solutions.size() != 0 ;
	}

	@Override
	public int getMinimumPoints() {
		return 3;
	}
}
