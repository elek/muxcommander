/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 22.05.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 * 
 * 
 * the unrar licence applies to all junrar source and binary distributions 
 * you are not allowed to use this source to re-create the RAR compression algorithm
 *
 * Here some html entities which can be used for escaping javadoc tags:
 * "&":  "&#038;" or "&amp;"
 * "<":  "&#060;" or "&lt;"
 * ">":  "&#062;" or "&gt;"
 * "@":  "&#064;" 
 */
package com.mucommander.commons.file.impl.rar.provider.de.innosystec.unrar.rarfile;

import com.mucommander.commons.file.impl.rar.provider.de.innosystec.unrar.io.Raw;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Calendar;
import java.util.Date;

/**
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class FileHeader extends BlockHeader {

	private Log logger = LogFactory.getLog(FileHeader.class);

	private static final byte SALT_SIZE = 8;

	private static final byte NEWLHD_SIZE = 32;

	private int unpSize;

	private HostSystem hostOS;

	private int fileCRC;

	private int fileTime;

	private byte unpVersion;

	private byte unpMethod;

	private short nameSize;

	private int highPackSize;

	private int highUnpackSize;

//	private byte[] fileName;

	private byte[] fileName;
	private String fileNameW;

	private byte[] subData;

	private byte[] salt = new byte[SALT_SIZE];

	private Date mTime;

	private Date cTime;

	private Date aTime;

	private Date arcTime;

	private long fullPackSize;

	private long fullUnpackSize;

	private int fileAttr;

	private int subFlags; // same as fileAttr (in header)

	private int recoverySectors = -1;
	
	public FileHeader(BlockHeader bh, byte[] fileHeader) {
		super(bh);

		int position = 0;
		unpSize = Raw.readIntLittleEndian(fileHeader, position);
		position+=4;
		hostOS = HostSystem.findHostSystem(fileHeader[4]);
		position++;

		fileCRC = Raw.readIntLittleEndian(fileHeader, position);
		position+=4;

		fileTime = Raw.readIntLittleEndian(fileHeader, position);
		position+=4;

		unpVersion |= fileHeader[13]&0xff;
		position++;
		unpMethod |= fileHeader[14]&0xff;
		position++;
		nameSize = Raw.readShortLittleEndian(fileHeader, position);
		position+=2;

		fileAttr = Raw.readIntLittleEndian(fileHeader, position);
		position+=4;
		if (isLargeBlock()) {
			highPackSize =Raw.readIntLittleEndian(fileHeader, position);
			position+=4;

			highUnpackSize = Raw.readIntLittleEndian(fileHeader, position);
			position+=4;
		} else {
			highPackSize = 0;
			highUnpackSize = 0;
			if (unpSize == 0xffffffff) {

				unpSize = 0xffffffff;
				highUnpackSize = Integer.MAX_VALUE;
			}

		}
		fullPackSize |= highPackSize;
		fullPackSize <<= 32;
		fullPackSize |= getPackSize();

		fullUnpackSize |= highUnpackSize;
		fullUnpackSize <<= 32;
		fullUnpackSize |= unpSize;

		nameSize = nameSize > 4 * 1024 ? 4 * 1024 : nameSize;

		fileName = new byte[nameSize];
		for (int i = 0; i < nameSize; i++) {
			fileName[i]=fileHeader[position];
			position++;
		}

		if(isFileHeader()){ 
			if(isUnicode()){
				int length = 0; 
//				fileName = "";
				fileNameW = ""; 
				while(length<fileName.length && fileName[length]!=0){
					length++; 					
				}
				byte[] name = new byte[length];
				System.arraycopy(fileName, 0, name, 0, name.length);				
				if(length!=nameSize){
					length++;
					fileNameW = FileNameDecoder.decode(fileName, length);
				}
				else
					fileNameW = new String(name);
			}
			else {
				fileNameW = new String(fileName);
			}
		} 

		if (UnrarHeadertype.NewSubHeader.equals(headerType)) {
			int datasize = headerSize - NEWLHD_SIZE - nameSize;
			if (hasSalt()) {
				datasize -= SALT_SIZE;
			}
			if (datasize > 0) {
				subData = new byte[datasize];
				for (int i = 0; i < datasize; i++) {
					subData[i] = (fileHeader[position]);
					position++;
				}
			}

			if (NewSubHeaderType.SUBHEAD_TYPE_RR.byteEquals(fileName)) {
				recoverySectors=subData[8]+(subData[9]<<8)+(subData[10]<<16)+(subData[11]<<24);
			}
		}

		if(hasSalt()){
			for (int i = 0; i < SALT_SIZE; i++) {
				salt[i] = fileHeader[position];
				position++;
			}
		}
		mTime = getDateDos(fileTime);
		//TODO rartime -> extended
	}
	
	private int strlen(byte[] filename) {
		int i=0;
		for (; i < nameSize; ++i)			
			if (((char) (fileName[i]&0xff)) == '\0')
				break;			
		
		return i;
	}

	@Override
    public void print(){
		super.print();
		StringBuilder str = new StringBuilder();
		str.append("unpSize: "+getUnpSize());
		str.append("\nHostOS: "+hostOS);
		str.append("\nMDate: "+mTime);
		str.append("\nFileName: "+getFileNameString());
		str.append("\nunpMethod: "+Integer.toHexString(getUnpMethod()));
		str.append("\nunpVersion: "+Integer.toHexString(getUnpVersion()));
		str.append("\nfullpackedsize: "+getFullPackSize());
		str.append("\nfullunpackedsize: "+getFullUnpackSize());
		str.append("\nisEncrypted: "+isEncrypted());
		str.append("\nisfileHeader: "+isFileHeader());
		str.append("\nisSolid: "+isSolid());
		str.append("\nisSplitafter: "+isSplitAfter());
		str.append("\nisSplitBefore:"+isSplitBefore());
		str.append("\nunpSize: "+getUnpSize());
		str.append("\ndataSize: "+getDataSize());
		str.append("\nisUnicode: "+isUnicode());
		str.append("\nhasVolumeNumber: "+hasVolumeNumber());
		str.append("\nhasArchiveDataCRC: "+hasArchiveDataCRC());
		str.append("\nhasSalt: "+hasSalt());
		str.append("\nhasEncryptVersions: "+hasEncryptVersion());
		str.append("\nisSubBlock: "+isSubBlock());
		logger.info(str.toString());
	}

	private Date getDateDos(int time)
	{
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR,(time>>>25)+1980 );
		cal.set(Calendar.MONTH, ((time>>>21) & 0x0f)-1);
		cal.set(Calendar.DAY_OF_MONTH, (time>>>16) & 0x1f);
		cal.set(Calendar.HOUR_OF_DAY, (time>>>11) & 0x1f);
		cal.set(Calendar.MINUTE, (time>>>5) & 0x3f);
		cal.set(Calendar.SECOND, (time & 0x1f)*2);
		return cal.getTime();
	}

	public Date getArcTime() {
		return arcTime;
	}

	public void setArcTime(Date arcTime) {
		this.arcTime = arcTime;
	}

	public Date getATime() {
		return aTime;
	}

	public void setATime(Date time) {
		aTime = time;
	}

	public Date getCTime() {
		return cTime;
	}

	public void setCTime(Date time) {
		cTime = time;
	}

	public int getFileAttr() {
		return fileAttr;
	}

	public void setFileAttr(int fileAttr) {
		this.fileAttr = fileAttr;
	}

	public int getFileCRC() {
		return fileCRC;
	}

	public String getFileNameString(){
		return fileNameW;
	}
	
	/*public byte[] getFileNameByteArray() {
		return fileName;
	}	

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}*/

	public String getFileNameW() {
		return fileNameW;
	}

	public void setFileNameW(String fileNameW) {
		this.fileNameW = fileNameW;
	}

	public int getHighPackSize() {
		return highPackSize;
	}

	public int getHighUnpackSize() {
		return highUnpackSize;
	}

	public HostSystem getHostOS() {
		return hostOS;
	}

	public Date getMTime() {
		return mTime;
	}

	public void setMTime(Date time) {
		mTime = time;
	}

	public short getNameSize() {
		return nameSize;
	}

	public int getRecoverySectors() {
		return recoverySectors;
	}

	public byte[] getSalt() {
		return salt;
	}

	public byte[] getSubData() {
		return subData;
	}

	public int getSubFlags() {
		return subFlags;
	}

	public byte getUnpMethod() {
		return unpMethod;
	}

	public int getUnpSize() {
		return unpSize;
	}

	public byte getUnpVersion() {
		return unpVersion;
	}

	public long getFullPackSize() {
		return fullPackSize;
	}

	public long getFullUnpackSize() {
		return fullUnpackSize;
	}

	public String toString() {
		return super.toString();
	}

	/**
	 * the file will be continued in the next archive part
	 */
	public boolean isSplitAfter()
	{
		return (this.flags & BlockHeader.LHD_SPLIT_AFTER)!=0;
	}
	
	/**
	 * the file is continued in this archive
	 */
	public boolean isSplitBefore(){
		return (this.flags & LHD_SPLIT_BEFORE)!=0;
	}
	/**
	 * this file is compressed as solid (all files handeled as one)
	 */
	public boolean isSolid(){
		return (this.flags & LHD_SOLID)!=0;
	}
	
	/**
	 * the file is encrypted
	 */
	public boolean isEncrypted(){
		return (this.flags & BlockHeader.LHD_PASSWORD)!=0;
	}
	
	/**
	 * the filename is also present in unicode
	 */
	public boolean isUnicode(){
		return (flags & LHD_UNICODE)!=0;
	}
	
	public boolean isFileHeader(){
		return  UnrarHeadertype.FileHeader.equals(headerType);
	}
	
	public boolean hasSalt() {
		return (flags & LHD_SALT) != 0;
	}
	
	public boolean isLargeBlock() {
		return (flags & LHD_LARGE) != 0;
	}

	/**
	 * whether this fileheader represents a directory
	 */
	public boolean isDirectory(){
		return (flags & LHD_WINDOWMASK) == LHD_DIRECTORY;
	}
}
