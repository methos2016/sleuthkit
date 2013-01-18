/*
 * Sleuth Kit Data Model
 *
 * Copyright 2011 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sleuthkit.datamodel;

import java.io.IOException;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sleuthkit.datamodel.TskData.TSK_FS_META_MODE_ENUM;

/**
 * ReprDataModel writes a String representation (containing the results of all
 * public method calls) of a Content object and its children to the given
 * Appendable.
 *
 * @author pmartel
 */
public class ReprDataModel {

	int indentLevel = 0;
	Appendable result, leaves;
	ContentVisitor reprVisitor = new ReprVisitor();
	static final int READ_BUFFER_SIZE = 8192;
	static final String HASH_ALGORITHM = "MD5";

	/**
	 * 
	 * @param result what to append the generated representation to.
	 */
	ReprDataModel(Appendable result,Appendable leaves) {
		this.result = result;
		this.leaves= leaves;
	}

	/**
	 * Entry point to represent a Content object and it's children, sets up the 
	 * sequential run method
	 * @param c the root Content object
	 */
	public void start(List<Content> lc) {
		List<Long> lp=new ArrayList<Long>();
		topDownDF(lc,lp);
	}
	
	private void topDownDF(List<Content> lc, List<Long> lp)
	{
		for(Content c : lc) {
			title(c.getClass().getSimpleName());
			c.accept(reprVisitor);
			readContent(c);
			lp.add(0,c.getId());
			try {
				if (c.getChildren().isEmpty())
				{
					append(lp.toString()+"\n", leaves);
				}
				else
				{
					topDownDF(c.getChildren(),new ArrayList<Long>(lp));
				}
			} catch (TskCoreException ex) {
				throw new RuntimeException(ex);
			}
			tail();
			lp.remove(0);
		}
	}
	/**
	 * Creates a top down representation of a database
	 * @param c the root Content object
	 */
	public void topDown(List<Content> lc)
	{
		List<Content> nex=new ArrayList<Content>();
		for(Content c: lc){
			title(c.getClass().getSimpleName());
			c.accept(reprVisitor);
			readContent(c);
			tail();
			try {
				if (c.getChildren().isEmpty())
				{
					append(c.getName()+": "+c.getId(),leaves);
				}
				else
				{
					nex.addAll(c.getChildren());
				}
			} catch (TskCoreException ex) {
				throw new RuntimeException(ex);
			}
		}
		indent();
		indentLevel++;
		if(!nex.isEmpty()){
			topDown(nex);}
	}
	private void title(String title) {
		indent();
		append(title, result);
		append(" >", result);
		indentLevel++;
		nl();
	}

	private void tail() {
		indentLevel--;
	}

	private void indent() {
		char[] indentation = new char[indentLevel];
		Arrays.fill(indentation, '\t');
		append(CharBuffer.wrap(indentation), result);
	}

	private void nl() {
		append("\n", result);
	}

	private void name(String name) {
		append(name, result);
		append(": ", result);
	}

	private void readContent(Content c) {
		/*long size = c.getSize();
		byte[] readBuffer = new byte[READ_BUFFER_SIZE];
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");

			for (long i = 0; i < size; i = i + READ_BUFFER_SIZE) {
				int read = c.read(readBuffer, i, Math.min(size - i, READ_BUFFER_SIZE));
				md5.update(readBuffer);
			}
			String hash = toHex(md5.digest());

			repr("read", "md5=" + hash);

		} catch (TskCoreException ex) {
			repr("read", ex);
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}*/
		nl();

	}

	private String toHex(byte[] bytes) {
		StringBuilder hex = new StringBuilder();
		for (byte b : bytes) {
			hex.append(String.format("%02x", b & 0xFF));
		}
		return hex.toString();
	}

	// Files and Directories can be handled the same
	private void reprFsContent(FsContent fsc) {
		repr("getAtime", fsc.getAtime());
		repr("getAtimeAsDate", fsc.getAtimeAsDate());
		repr("getAttr_id", fsc.getAttrId());
		repr("getAttr_type", fsc.getAttrType().toString());
		repr("getCrtime", fsc.getCrtime());
		repr("getCrtimeAsDate", fsc.getCrtimeAsDate());
		repr("getCtime", fsc.getCtime());
		repr("getCtimeAsDate", fsc.getCtimeAsDate());
		repr("getDirFlagsAsString", fsc.getDirFlagAsString());
		repr("getDirType", fsc.getDirType().getValue());
		repr("getDirTypeAsString", fsc.getDirTypeAsString());
		repr("getGid", fsc.getGid());
		repr("getMetaFlagsAsString", fsc.getMetaFlagsAsString());
		repr("getMetaTypeAsString", fsc.getMetaTypeAsString());
		repr("getMetaAddr", fsc.getMetaAddr());
		repr("getMetaType", fsc.getMetaType().getValue());
		repr("getMtime", fsc.getMtime());
		repr("getMtimeAsDate", fsc.getMtimeAsDate());
		repr("getName", fsc.getName());
		repr("getParentPath", fsc.getParentPath());
		repr("getSize", fsc.getSize());
		repr("getUid", fsc.getUid());
	}
	
	private void reprLayoutFile(LayoutFile lf) {
		repr("getSize", lf.getSize());
		repr("getId", lf.getId());
		repr("getName", lf.getName());
		repr("getNumPartsu", (long)lf.getNumParts());
		
	}
	
	private void reprLayoutDirectory(VirtualDirectory ld) {
		repr("getSize", ld.getSize());
		repr("getId", ld.getId());
		repr("getName", ld.getName());
	}

	private void reprFileSystem(FileSystem fs) {
		repr("getBlock_count", fs.getBlock_count());
		repr("getBlock_size", fs.getBlock_size());
		
		/* 
		 * Don't get handle, it's not consistent (a memory pointer).
		 * 
		try {
			repr("getFileSystemHandle", fs.getFileSystemHandle());
		} catch (TskException ex) {
			throw new RuntimeException(ex);
		}
		 * 
		 */
		repr("getFirst_inum", fs.getFirst_inum());
		repr("getFs_type", fs.getFsType().getValue());
		repr("getImg_offset", fs.getImageOffset());
		repr("getLast_inum", fs.getLastInum());
		repr("getRoot_inum", fs.getRoot_inum());
		repr("getSize", fs.getSize());
	}

	private void reprImage(Image i) {
		/* 
		 * Don't get handle, it's not consistent (a memory pointer).
		 * 
		repr("getImageHandle", i.getImageHandle());
		 * 
		 */
		repr("getName", i.getName());
		/**
		 * Don't get paths, they're system specific.
		 * 
		repr("getPaths", i.getPaths());
		 * 
		 */
		repr("getSize", i.getSize());
		repr("getSsize", i.getSsize());
		repr("getType", i.getType());
		try {
			int typeID = i.getSleuthkitCase().addArtifactType("Test_Artifact", "Test Artifact");
			BlackboardArtifact art1;
			BlackboardArtifact art2;
			BlackboardArtifact art3;
			art1 = i.newArtifact(BlackboardArtifact.ARTIFACT_TYPE.TSK_GEN_INFO);
			art2 = i.newArtifact(typeID);
			art3 = i.newArtifact(1);
			byte[] bytearray1 = new byte[2];
			bytearray1[0] = 1;
			bytearray1[1] = 2;
			byte[] bytearray2 = new byte[2];
			bytearray2[0] = 3;
			bytearray2[1] = 4;
			byte[] bytearray3 = new byte[2];
			bytearray3[0] = 5;
			bytearray3[1] = 6;
			
			int attrTypeID = i.getSleuthkitCase().addAttrType("testattr", "Test Attribute");
			
			art1.addAttribute(new BlackboardAttribute(attrTypeID, "regressionTest", "first_call", (int) 23));
			art1.addAttribute(new BlackboardAttribute(attrTypeID, "regressionTest", "second_call", (long) 5));
			art1.addAttribute(new BlackboardAttribute(attrTypeID, "regressionTest", "third_call", (double) 7.412));
			art1.addAttribute(new BlackboardAttribute(attrTypeID, "regressionTest", "fourth_call", "test"));
			art1.addAttribute(new BlackboardAttribute(attrTypeID, "regressionTest", "fifth_call", bytearray1));
			art2.addAttribute(new BlackboardAttribute(BlackboardAttribute.ATTRIBUTE_TYPE.TSK_DATETIME.getTypeID(), "regressionTest", "sixth_call", (int) 23));
			art2.addAttribute(new BlackboardAttribute(BlackboardAttribute.ATTRIBUTE_TYPE.TSK_FLAG.getTypeID(), "regressionTest", "seventh_call", (long) 5));
			art2.addAttribute(new BlackboardAttribute(BlackboardAttribute.ATTRIBUTE_TYPE.TSK_GEO_ALTITUDE.getTypeID(), "regressionTest", "eighth_call", (double) 7.412));
			art2.addAttribute(new BlackboardAttribute(BlackboardAttribute.ATTRIBUTE_TYPE.TSK_KEYWORD.getTypeID(), "regressionTest", "nineth_call", "test"));
			art2.addAttribute(new BlackboardAttribute(BlackboardAttribute.ATTRIBUTE_TYPE.TSK_KEYWORD_SET.getTypeID(), "regressionTest", "tenth_call", bytearray2));
			art3.addAttribute(new BlackboardAttribute(1, "regressionTest", "eleventh_call", (int) 29));
			art3.addAttribute(new BlackboardAttribute(1, "regressionTest", "twelfth_call", (long) 565413));
			art3.addAttribute(new BlackboardAttribute(1, "regressionTest", "thirteenth_call", (double) 1.987));
			art3.addAttribute(new BlackboardAttribute(2, "regressionTest", "fourteenth_call", "test2"));
			art3.addAttribute(new BlackboardAttribute(2, "regressionTest", "fifteenth_call", bytearray3));
			
			for(BlackboardArtifact art : i.getAllArtifacts()){
				repr("ArtifactGetArtifactID", art.getArtifactID());
				repr("ArtifactGetArtifactTypeID", new Integer(art.getArtifactTypeID()).toString());
				repr("ArtifactGetArtifactTypeName", art.getArtifactTypeName());
				repr("ArtifactGetDisplayName", art.getDisplayName());
				repr("ArtifactGetObjectID", art.getObjectID());
				for(BlackboardAttribute attr : art.getAttributes()){
					repr("AttributeGetArtifactID", attr.getArtifactID());
					repr("AttributeGetAttributeTypeID", new Integer(attr.getAttributeTypeID()).toString());
					repr("AttributeGetAttributeTypeName", i.getSleuthkitCase().getAttrTypeString(attr.getAttributeTypeID()));
					repr("AttributeGetDisplayName", i.getSleuthkitCase().getAttrTypeDisplayName(attr.getAttributeTypeID()));
					repr("AttributeGetContext", attr.getContext());
					repr("AttributeGetSource", attr.getModuleName());
					BlackboardAttribute.TSK_BLACKBOARD_ATTRIBUTE_VALUE_TYPE val_type = attr.getValueType();
					repr("AttributeGetValueType", val_type.getLabel());
					switch(val_type){
						case STRING:
							repr("AttributeGetValue" + val_type.getLabel(), attr.getValueString());
							break;
						case BYTE:
							repr("AttributeGetValue" + val_type.getLabel(), Arrays.toString(attr.getValueBytes()));
							break;
						case INTEGER:
							repr("AttributeGetValue" + val_type.getLabel(), new Integer(attr.getValueInt()).toString());
							break;
						case LONG:
							repr("AttributeGetValue" + val_type.getLabel(), attr.getValueLong());
							break;
						case DOUBLE:
							repr("AttributeGetValue" + val_type.getLabel(), new Double(attr.getValueDouble()).toString());
							break;
					}
				}
			}
			
		} catch (TskCoreException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void reprVolume(Volume v) {
		repr("getAddr", v.getAddr());
		repr("getDescription", v.getDescription());
		repr("getFlags", v.getFlags());
		repr("getFlagsAsString", v.getFlagsAsString());
		repr("getLength", v.getLength());
		repr("getSize", v.getSize());
		repr("getStart", v.getStart());
	}

	private void reprVolumeSystem(VolumeSystem vs) {
		repr("getBlockSize", vs.getBlockSize());
		repr("getOffset", vs.getOffset());
		repr("getSize", vs.getSize());
		repr("getType", vs.getType());
		/* 
		 * Don't get handle, it's not consistent (a memory pointer).
		 * 
		try {
			repr("getVolumeSystemHandle", vs.getVolumeSystemHandle());
		} catch (TskException ex) {
			throw new RuntimeException(ex);
		}
		 * 
		 */
	}

	private void repr(String method, Long l) {
		indent();
		name(method);
		append(l.toString(), result);
		nl();
	}
	
	private void repr(String method, Integer l) {
		indent();
		name(method);
		append(l.toString(), result);
		nl();
	}
	
	private void repr(String method, Short l) {
		indent();
		name(method);
		append(l.toString(), result);
		nl();
	}
	
	private void repr(String method, Byte l) {
		indent();
		name(method);
		append(l.toString(), result);
		nl();
	}

	private void repr(String method, String[] sArray) {
		indent();
		name(method);
		append(Arrays.toString(sArray), result);
		nl();
	}

	private void repr(String method, String s) {
		indent();
		name(method);
		append(s, result);
		nl();
	}

	private void repr(String method, Exception ex) {
		indent();
		name(method);
		nl();
		append(ex.toString(), result);
		nl();
	}

	private void append(CharSequence s, Appendable f) {
		try {
			//System.out.append(s);
			//System.out.flush();
			f.append(s);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	private class ReprVisitor implements ContentVisitor<Void> {

		@Override
		public Void visit(LayoutFile u) {
			reprLayoutFile(u);
			return null;
		}
		
		@Override
		public Void visit(VirtualDirectory u) {
			reprLayoutDirectory(u);
			return null;
		}
		
		

		@Override
		public Void visit(Directory d) {
			reprFsContent(d);
			return null;
		}

		@Override
		public Void visit(File f) {
			reprFsContent(f);
			return null;
		}

		@Override
		public Void visit(FileSystem fs) {
			reprFileSystem(fs);
			return null;
		}

		@Override
		public Void visit(Image i) {
			reprImage(i);
			return null;
		}

		@Override
		public Void visit(Volume v) {
			reprVolume(v);
			return null;
		}

		@Override
		public Void visit(VolumeSystem vs) {
			reprVolumeSystem(vs);
			return null;
		}
	}
}
