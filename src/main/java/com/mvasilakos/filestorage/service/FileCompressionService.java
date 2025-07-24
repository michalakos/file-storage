package com.mvasilakos.filestorage.service;

import com.mvasilakos.filestorage.exception.FileCompressionException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.springframework.stereotype.Service;

/**
 * File compression service.
 */
@Service
public class FileCompressionService {

  /**
   * Compress byte array.
   *
   * @param data uncompressed data
   * @return compressed data
   */
  public byte[] compress(byte[] data) {
    try {
      ByteArrayOutputStream compressedBytes = new ByteArrayOutputStream();
      try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(compressedBytes)) {
        gzipOutputStream.write(data);
        gzipOutputStream.finish();
      }
      return compressedBytes.toByteArray();
    } catch (IOException e) {
      throw new FileCompressionException("Failed to compress data", e);
    }
  }

  /**
   * Decompress byte array.
   *
   * @param compressedData compressed data
   * @return decompressed data
   */
  public byte[] decompress(byte[] compressedData) {
    try {
      ByteArrayOutputStream decompressedBytes = new ByteArrayOutputStream();
      try (ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedData);
          GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream)) {
        gzipInputStream.transferTo(decompressedBytes);
      }
      return decompressedBytes.toByteArray();
    } catch (IOException e) {
      throw new FileCompressionException("Failed to decompress data", e);
    }
  }

}