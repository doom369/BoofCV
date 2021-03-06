/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.quadblob;

import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestFindQuadCorners {

	/**
	 * Overall test which tests all functions at once
	 */
	@Test
	public void process() {
		List<Point2D_I32> contour = new ArrayList<Point2D_I32>();
		
		for( int i = 0; i < 10; i++ ) {
			contour.add( new Point2D_I32(i,0));
			contour.add( new Point2D_I32(i,9));
		}
		for( int i = 1; i < 9; i++ ) {
			contour.add( new Point2D_I32(0,i));
			contour.add( new Point2D_I32(9,i));
		}

		// remove any structure from the input
		Collections.shuffle(contour,new Random(1234));
		
		FindQuadCorners alg = new FindQuadCorners();

		List<Point2D_I32> corners = alg.process(contour);

		// check the solution and make sure its in the correct order
		Point2D_I32 a = corners.get(0);
		Point2D_I32 b = corners.get(1);
		Point2D_I32 c = corners.get(2);
		Point2D_I32 d = corners.get(3);

		assertEquals(0,a.x);
		assertEquals(0,a.y);

		assertEquals(9,b.x);
		assertEquals(0,b.y);

		assertEquals(9,c.x);
		assertEquals(9,c.y);

		assertEquals(0,d.x);
		assertEquals(9,d.y);
	}


}
