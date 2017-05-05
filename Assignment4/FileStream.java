import java.io.*;
import java.nio.*;

/*****************************/

/**
 * \brief class for reading and writing
 *       files
 *
 *
 **********************************/
public class FileStream extends InputStream implements Serializable {
private int currentPosition;
private byte[] byteBuffer;
private int size;
/*****************************/

/**
 * \brief constructor for filestream
 * \param pathName the location where
 *      file is saved in
 *
 **********************************/
public FileStream(String pathName) throws FileNotFoundException, IOException    {
    File file = new File(pathName);

    size = (int)file.length();
    byteBuffer = new byte[size];
    FileInputStream	fileInputStream = new FileInputStream(pathName);
    int			i = 0;
    while (fileInputStream.available() > 0)
	byteBuffer[i++] = (byte)fileInputStream.read();
    fileInputStream.close();
    currentPosition = 0;
}

/*****************************/

/**
 * \brief empty constructor for
 *       filestream
 *
 **********************************/
public FileStream() throws FileNotFoundException    {
    currentPosition = 0;
}

/*****************************/

/**
 * \brief  read the file
 *
 *
 **********************************/
public int read() throws IOException {
    if (currentPosition < size)
	return (int)byteBuffer[currentPosition++];
    return 0;
}

/*****************************/

/**
 * \brief used to check if file is available
 *
 *
 **********************************/
public int available() throws IOException {
    return size - currentPosition;
}
}
