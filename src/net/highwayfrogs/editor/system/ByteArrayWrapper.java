package net.highwayfrogs.editor.system;

/**
 * A wrapper around byte[].
 * Created by Kneesnap on 11/6/2018.
 */
public class ByteArrayWrapper {
    private byte[] array;
    private int length;
    private int growthFactor;

    public ByteArrayWrapper(int initialSize) {
        this(initialSize, 0);
    }

    public ByteArrayWrapper(int initialSize, int growthFactor) {
        this.array = new byte[initialSize];
        this.growthFactor = growthFactor;
    }

    /**
     * Copy the contents of this wrapper to a byte array.
     * @param copyArray The array to copy contents into.
     * @return array
     */
    public byte[] toArray(byte[] copyArray, int destIndex) {
        System.arraycopy(this.array, 0, copyArray, destIndex, Math.min(copyArray.length - destIndex, arraySize()));
        return copyArray;
    }

    /**
     * Resize the underlying array.
     * @param newSize The underlying array's new size.
     */
    public void resize(int newSize) {
        this.array = toArray(new byte[newSize], 0);
    }

    /**
     * Add a byte to this array.
     * @param value The value to add.
     */
    public void add(byte value) {
        if (this.growthFactor > 0 && this.length >= arraySize()) // We've reached the end of our array, it needs to expand.
            resize(arraySize() + this.growthFactor);

        this.array[this.length++] = value;
    }

    /**
     * Clear all values from this array.
     */
    public void clear() {
        this.length = 0;
    }

    /**
     * Clear the contents of this array and expand it.
     * @param newSize The size to expand to. (Silently does nothing if it's larger than the current size)
     */
    public void clearExpand(int newSize) {
        clear();
        if (newSize > arraySize())
            resize(newSize);
    }

    /**
     * Get the value at a given index.
     * @param index The index to get.
     * @return value
     */
    public byte get(int index) {
        return this.array[index];
    }

    /**
     * Gets the amount of elements in this array.
     * @return elementCount
     */
    public int size() {
        return this.length;
    }

    /**
     * Gets the size of the underlying array.
     * @return arraySize
     */
    public int arraySize() {
        return this.array.length;
    }
}