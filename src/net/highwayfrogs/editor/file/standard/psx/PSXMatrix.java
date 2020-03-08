package net.highwayfrogs.editor.file.standard.psx;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a MR_MAT struct, which is based on the PSX "MATRIX" struct in libgte.h.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PSXMatrix extends GameObject {
    private short[][] matrix = new short[DIMENSION][DIMENSION]; // 3x3 Rotation Matrix.
    private int[] transform = new int[DIMENSION]; // Transform vector.
    private short padding; // This is not in the struct. It is present in all tested versions, so it may be added automatically by the compiler.

    private static final int DIMENSION = 3;
    public static final int BYTE_SIZE = (DIMENSION * DIMENSION * Constants.SHORT_SIZE) + (DIMENSION * Constants.INTEGER_SIZE);

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                this.matrix[i][j] = reader.readShort();

        for (int i = 0; i < this.transform.length; i++)
            this.transform[i] = reader.readInt();

        this.padding = reader.readShort();
        if (this.padding != 0 && this.padding != -1)
            throw new RuntimeException("Expected a padding value!");
    }

    @Override
    public void save(DataWriter writer) {
        for (short[] aMatrix : this.matrix)
            for (short aShort : aMatrix)
                writer.writeShort(aShort);

        for (int aTransfer : this.transform)
            writer.writeInt(aTransfer);

        writer.writeShort(this.padding);
    }

    /**
     * Get the transform as an SVector.
     * @return transformVector
     */
    public SVector toVector() {
        return new SVector((short) getTransform()[0], (short) getTransform()[1], (short) getTransform()[2]);
    }

    /**
     * Gets the yaw from this matrix.
     * @return yaw
     */
    public double getYawAngle() {
        double r31 = clampPi(Utils.fixedPointShortToFloat12Bit(getMatrix()[2][0]));

        if (r31 == 1 || r31 == -1) { // Gymbal lock at pitch = -90 or 90
            return 0F;
        } else {
            double r11 = clampPi(Utils.fixedPointShortToFloat12Bit(getMatrix()[0][0]));
            double r21 = clampPi(Utils.fixedPointShortToFloat12Bit(getMatrix()[1][0]));
            return Math.atan2(r21, r11);
        }
    }

    /**
     * Gets the pitch from this matrix.
     * @return pitch
     */
    public double getPitchAngle() {
        float r31 = Utils.fixedPointShortToFloat12Bit(getMatrix()[2][0]);
        return Math.asin(clampPi(-r31));
    }

    /**
     * Gets the roll from this matrix.
     * @return roll
     */
    public double getRollAngle() {
        double r31 = clampPi(Utils.fixedPointShortToFloat12Bit(getMatrix()[2][0]));

        double r12 = clampPi(Utils.fixedPointShortToFloat12Bit(getMatrix()[0][1]));
        double r13 = clampPi(Utils.fixedPointShortToFloat12Bit(getMatrix()[0][2]));
        if (r31 == 1) { // Gymbal lock at pitch = -90
            return Math.atan2(-r12, -r13);
        } else if (r31 == -1) { // Lock at pitch = 90
            return Math.atan2(r12, r13);
        } else {
            double r32 = clampPi(Utils.fixedPointShortToFloat12Bit(getMatrix()[2][1]));
            double r33 = clampPi(Utils.fixedPointShortToFloat12Bit(getMatrix()[2][2]));
            return Math.atan2(r32, r33);
        }
    }

    private static double clampPi(float num) {
        boolean positive = num >= 0;
        return Math.abs(Math.PI - num) < 0.000001 ? (positive ? Math.PI : -Math.PI) : num;
    }

    /**
     * Update the matrix.
     * Useful: http://danceswithcode.net/engineeringnotes/rotations_in_3d/rotations_in_3d_part2.html
     * @param yaw   The new pitch.
     * @param pitch The new yaw.
     * @param roll  The new roll.
     */
    public void updateMatrix(double yaw, double pitch, double roll) {
        if (Math.abs(pitch) >= Math.PI / 2)
            return; // LOCK! ABORT!

        double su = Math.sin(roll);
        double cu = Math.cos(roll);
        double sv = Math.sin(pitch);
        double cv = Math.cos(pitch);
        double sw = Math.sin(yaw);
        double cw = Math.cos(yaw);

        // Update rotation matrix.
        this.matrix[0][0] = Utils.floatToFixedPointShort12Bit((float) (cv * cw)); // r11
        this.matrix[0][1] = Utils.floatToFixedPointShort12Bit((float) (su * sv * cw - cu * sw)); // r12
        this.matrix[0][2] = Utils.floatToFixedPointShort12Bit((float) (su * sw + cu * sv * cw)); // r13
        this.matrix[1][0] = Utils.floatToFixedPointShort12Bit((float) (cv * sw)); // r21
        this.matrix[1][1] = Utils.floatToFixedPointShort12Bit((float) (cu * cw + su * sv * sw)); // r22
        this.matrix[1][2] = Utils.floatToFixedPointShort12Bit((float) (cu * sv * sw - su * cw)); // r23
        this.matrix[2][0] = Utils.floatToFixedPointShort12Bit((float) -sv); // r31
        this.matrix[2][1] = Utils.floatToFixedPointShort12Bit((float) (su * cv)); // r32
        this.matrix[2][2] = Utils.floatToFixedPointShort12Bit((float) (cu * cv)); // r33
    }

    /**
     * Equivalent to ApplyRotMatrix.
     * @param rotationMatrix The matrix to get rotation data from.
     * @param input          The coordinate input.
     * @param output         Where to write the output.
     * @return rotMatrix
     */
    public static IVector MRApplyRotMatrix(PSXMatrix rotationMatrix, SVector input, IVector output) {
        output.setX(((rotationMatrix.matrix[0][0] * input.getX()) + (rotationMatrix.matrix[0][1] * input.getY()) + (rotationMatrix.matrix[0][2] * input.getZ())) >> 12);
        output.setY(((rotationMatrix.matrix[1][0] * input.getX()) + (rotationMatrix.matrix[1][1] * input.getY()) + (rotationMatrix.matrix[1][2] * input.getZ())) >> 12);
        output.setZ(((rotationMatrix.matrix[2][0] * input.getX()) + (rotationMatrix.matrix[2][1] * input.getY()) + (rotationMatrix.matrix[2][2] * input.getZ())) >> 12);
        return output;
    }


    /**
     * Equivalent to MulMatrix0. (http://psxdev.tlrmcknz.com/psyq/ref/libref46/0441.html?sidebar=outlines)
     * @param input1 An input matrix.
     * @param input2 Another input matrix.
     * @param output Output matrix.
     */
    public static void MRMulMatrixABC(PSXMatrix input1, PSXMatrix input2, PSXMatrix output) {
        PSXMatrix tmpMtx = new PSXMatrix();
        tmpMtx.setTransform(output.getTransform());

        // Clear matrix (IMPORTANT!)
        for (int i = 0; i < DIMENSION; i++)
            for (int j = 0; j < DIMENSION; j++)
                tmpMtx.matrix[i][j] = 0;

        // Perform multiplication
        for (int i = 0; i < DIMENSION; i++)
            for (int j = 0; j < DIMENSION; j++)
                for (int k = 0; k < DIMENSION; k++)
                    tmpMtx.matrix[i][j] += ((input2.matrix[k][j] * input1.matrix[i][k]) >> 12);

        // Copy values across to output matrix
        output.setTransform(tmpMtx.getTransform());
        output.setMatrix(tmpMtx.getMatrix());
    }

    /**
     * Equivalent to ApplyMatrix. matrix -> vec (http://psxdev.tlrmcknz.com/psyq/ref/libref46/0392.html?sidebar=outlines)
     * @param matrix The matrix to multiply.
     * @param vector The short vector to multiply the matrix by.
     * @param output The output to write to.
     */
    public static IVector MRApplyMatrix(PSXMatrix matrix, SVector vector, IVector output) {
        output.setX((((matrix.matrix[0][0] * vector.getX()) + (matrix.matrix[0][1] * vector.getY()) + (matrix.matrix[0][2] * vector.getZ())) >> 12) + matrix.transform[0]);
        output.setY((((matrix.matrix[1][0] * vector.getX()) + (matrix.matrix[1][1] * vector.getY()) + (matrix.matrix[1][2] * vector.getZ())) >> 12) + matrix.transform[1]);
        output.setZ((((matrix.matrix[2][0] * vector.getX()) + (matrix.matrix[2][1] * vector.getY()) + (matrix.matrix[2][2] * vector.getZ())) >> 12) + matrix.transform[2]);
        return output;
    }

    public static PSXMatrix WriteAxesAsMatrix(PSXMatrix matrix, IVector vecX, IVector vecY, IVector vecZ) {
        matrix.getMatrix()[0][0] = (short) vecX.getX();
        matrix.getMatrix()[1][0] = (short) vecX.getY();
        matrix.getMatrix()[2][0] = (short) vecX.getZ();
        matrix.getMatrix()[0][1] = (short) vecY.getX();
        matrix.getMatrix()[1][1] = (short) vecY.getY();
        matrix.getMatrix()[2][1] = (short) vecY.getZ();
        matrix.getMatrix()[0][2] = (short) vecZ.getX();
        matrix.getMatrix()[1][2] = (short) vecZ.getY();
        matrix.getMatrix()[2][2] = (short) vecZ.getZ();
        return matrix;
    }

    @Override
    public String toString() {
        return "PsxMatrix Pos[" + getTransform()[0] + ", " + getTransform()[1] + ", " + getTransform()[2] + "] Yaw: " + getYawAngle() + ", Pitch: " + getPitchAngle() + ", Roll: " + getRollAngle();
    }
}
