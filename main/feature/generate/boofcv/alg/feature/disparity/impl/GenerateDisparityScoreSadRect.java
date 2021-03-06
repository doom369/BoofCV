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

package boofcv.alg.feature.disparity.impl;

import boofcv.misc.AutoTypeImage;
import boofcv.misc.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class GenerateDisparityScoreSadRect extends CodeGeneratorBase {

	String typeInput;
	String dataAbr;
	String bitWise;
	String sumType;

	@Override
	public void generate() throws FileNotFoundException {
		createFile(AutoTypeImage.U8);
		createFile(AutoTypeImage.S16);
		createFile(AutoTypeImage.F32);
	}

	public void createFile( AutoTypeImage image ) throws FileNotFoundException {
		setOutputFile("ImplDisparityScoreSadRect_"+image.getAbbreviatedType());
		typeInput = image.getSingleBandName();
		bitWise = image.getBitWise();
		sumType = image.getSumType();

		dataAbr = image.isInteger() ? "S32" : "F32";

		printPreamble();
		printConstructor();
		printProcess();
		printComputeFirstRow();
		printComputeRemainingRows();
		printTheRest();

		out.println("}");
	}

	private void printPreamble() {
		out.print("import boofcv.alg.InputSanityCheck;\n" +
				"import boofcv.alg.feature.disparity.DisparityScoreSadRect;\n" +
				"import boofcv.alg.feature.disparity.DisparitySelect;\n" +
				"import boofcv.struct.image.ImageSingleBand;\n" +
				"import boofcv.struct.image."+typeInput+";\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Implementation of {@link boofcv.alg.feature.disparity.DisparityScoreSadRect} for processing\n" +
				" * input images of type {@link "+typeInput+"}.\n" +
				" * </p>\n" +
				" * <p>\n" +
				" * DO NOT MODIFY. Generated by {@link GenerateDisparityScoreSadRect}.\n" +
				" * </p>\n"+
				" * \n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"public class "+className+"<Disparity extends ImageSingleBand>\n" +
				"\textends DisparityScoreSadRect<"+typeInput+",Disparity>\n" +
				"{\n" +
				"\n" +
				"\t// Computes disparity from scores\n" +
				"\tDisparitySelect<"+sumType+"[],Disparity> computeDisparity;\n" +
				"\n" +
				"\t// stores the local scores for the width of the region\n" +
				"\t"+sumType+" elementScore[];\n" +
				"\t// scores along horizontal axis for current block\n" +
				"\t// To allow right to left validation all disparity scores are stored for the entire row\n" +
				"\t// size = num columns * maxDisparity\n" +
				"\t// disparity for column i is stored in elements i*maxDisparity to (i+1)*maxDisparity\n" +
				"\t"+sumType+" horizontalScore[][];\n" +
				"\t// summed scores along vertical axis\n" +
				"\t// This is simply the sum of like elements in horizontal score\n" +
				"\t"+sumType+" verticalScore[];\n\n");
	}

	private void printConstructor() {
		out.print("\tpublic "+className+"( int minDisparity , int maxDisparity,\n" +
				"\t\t\t\t\t\t\t\t\t\tint regionRadiusX, int regionRadiusY,\n" +
				"\t\t\t\t\t\t\t\t\t\tDisparitySelect<"+sumType+"[],Disparity> computeDisparity) {\n" +
				"\t\tsuper(minDisparity,maxDisparity,regionRadiusX,regionRadiusY);\n" +
				"\n" +
				"\t\tthis.computeDisparity = computeDisparity;\n" +
				"\t}\n\n");
	}

	private void printProcess() {
		out.print("\t@Override\n" +
				"\tpublic void _process( "+typeInput+" left , "+typeInput+" right , Disparity disparity ) {\n" +
				"\t\tif( horizontalScore == null || verticalScore.length < lengthHorizontal ) {\n" +
				"\t\t\thorizontalScore = new "+sumType+"[regionHeight][lengthHorizontal];\n" +
				"\t\t\tverticalScore = new "+sumType+"[lengthHorizontal];\n" +
				"\t\t\telementScore = new "+sumType+"[ left.width ];\n" +
				"\t\t}\n" +
				"\n" +
				"\t\tcomputeDisparity.configure(disparity,minDisparity,maxDisparity,radiusX);\n" +
				"\n" +
				"\t\t// initialize computation\n" +
				"\t\tcomputeFirstRow(left, right);\n" +
				"\t\t// efficiently compute rest of the rows using previous results to avoid repeat computations\n" +
				"\t\tcomputeRemainingRows(left, right);\n" +
				"\t}\n\n");
	}

	private void printComputeFirstRow() {
		out.print("\t/**\n" +
				"\t * Initializes disparity calculation by finding the scores for the initial block of horizontal\n" +
				"\t * rows.\n" +
				"\t */\n" +
				"\tprivate void computeFirstRow("+typeInput+" left, "+typeInput+" right ) {\n" +
				"\t\t// compute horizontal scores for first row block\n" +
				"\t\tfor( int row = 0; row < regionHeight; row++ ) {\n" +
				"\n" +
				"\t\t\t"+sumType+" scores[] = horizontalScore[row];\n" +
				"\n" +
				"\t\t\tUtilDisparityScore.computeScoreRow(left, right, row, scores,\n" +
				"\t\t\t\t\tminDisparity,maxDisparity,regionWidth,elementScore);\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// compute score for the top possible row\n" +
				"\t\tfor( int i = 0; i < lengthHorizontal; i++ ) {\n" +
				"\t\t\t"+sumType+" sum = 0;\n" +
				"\t\t\tfor( int row = 0; row < regionHeight; row++ ) {\n" +
				"\t\t\t\tsum += horizontalScore[row][i];\n" +
				"\t\t\t}\n" +
				"\t\t\tverticalScore[i] = sum;\n" +
				"\t\t}\n" +
				"\n" +
				"\t\t// compute disparity\n" +
				"\t\tcomputeDisparity.process(radiusY, verticalScore);\n" +
				"\t}\n\n");
	}

	private void printComputeRemainingRows() {
		out.print("\t/**\n" +
				"\t * Using previously computed results it efficiently finds the disparity in the remaining rows.\n" +
				"\t * When a new block is processes the last row/column is subtracted and the new row/column is\n" +
				"\t * added.\n" +
				"\t */\n" +
				"\tprivate void computeRemainingRows( "+typeInput+" left, "+typeInput+" right )\n" +
				"\t{\n" +
				"\t\tfor( int row = regionHeight; row < left.height; row++ ) {\n" +
				"\t\t\tint oldRow = row%regionHeight;\n" +
				"\n" +
				"\t\t\t// subtract first row from vertical score\n" +
				"\t\t\t"+sumType+" scores[] = horizontalScore[oldRow];\n" +
				"\t\t\tfor( int i = 0; i < lengthHorizontal; i++ ) {\n" +
				"\t\t\t\tverticalScore[i] -= scores[i];\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\tUtilDisparityScore.computeScoreRow(left, right, row, scores,\n" +
				"\t\t\t\t\tminDisparity,maxDisparity,regionWidth,elementScore);\n" +
				"\n" +
				"\t\t\t// add the new score\n" +
				"\t\t\tfor( int i = 0; i < lengthHorizontal; i++ ) {\n" +
				"\t\t\t\tverticalScore[i] += scores[i];\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\t// compute disparity\n" +
				"\t\t\tcomputeDisparity.process(row - regionHeight + 1 + radiusY, verticalScore);\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printTheRest() {
		out.print("\t@Override\n" +
				"\tpublic Class<"+typeInput+"> getInputType() {\n" +
				"\t\treturn "+typeInput+".class;\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic Class<Disparity> getDisparityType() {\n" +
				"\t\treturn computeDisparity.getDisparityType();\n" +
				"\t}\n\n");
	}

	public static void main( String args[] ) throws FileNotFoundException {
		GenerateDisparityScoreSadRect gen = new GenerateDisparityScoreSadRect();

		gen.generate();
	}
}
