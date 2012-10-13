/*
 * Created on 2004-7-18
 * Author: Xuefeng, Copyright (C) 2004, Xuefeng.
 */
package y.module;

import java.util.*;

import y.format.*;
import y.view.*;

import java.io.Serializable;


/**
 * Paragraph represent a paragraph of a document. 
 * <b>NOTE</b>: It is a basic glyph structure that 
 * do NOT contains physical structure. <br>
 * It must contains at least 1 Characters that is the end flag: '\r'. 
 * Once a paragraph object is created, the end flag '\r' is added automatically, 
 * and take care to operate the paragraph to make sure the end flag will 
 * never be deleted. 
 * 
 * @author Xuefeng
 */
public class Paragraph implements Composition, Serializable{


	/**
	 * 
	 */
	private static final long serialVersionUID = -8454505627673244373L;

	// store the reference of the document:
	private Document document;

	// store the "leaf" glyphs it contains (Char and Picture, etc.):
	private List glyphs = new ArrayList(5);

	// StringFormat are always synchronized with glyphs, 
	// NOTE StringFormat are managed by Paragraph itself: 
	private List stringFormats = new ArrayList<StringFormat>(1);
	
	// store the current stringFormat
	private StringFormat currentStringFormat;
	
	//indicate whether the stringFormat is changed
//	private boolean isChanged = false;

    // store the rows, this is available only after a 
    // Compositor.compose() invokation, so rows are managed 
	// by Compositor: 
    private List rows = new ArrayList(1);

    // this paragraph's Format:
    private ParagraphFormat paragraphFormat;

    // formatted or not:
    private boolean formatted = false;

    
    public Paragraph() {
        currentStringFormat =  new StringFormat(this,
                yFontFactory.instance().createDefaultEnglishFont(),
                yColor.BLACK, 0, 0);
    }
    /**
     * Create a new Paragraph object. It will compose later. 
     * 
     * @param document The top structure of the data.
     */
    public Paragraph(Document document) {
        //Assert.checkNull(document);
        this.document = document;
        // default paragraph:
        this.paragraphFormat = new ParagraphFormat(document);
        // make sure it is end with '\r':
        this.glyphs.add(Char.RETURN);
        // set up the default string format
        currentStringFormat =  new StringFormat(this,
                yFontFactory.instance().createDefaultEnglishFont(),
                yColor.BLACK, 0, 0);
        StringFormat defaultSF = new StringFormat(this,
                yFontFactory.instance().createDefaultEnglishFont(),
                yColor.BLACK, 0, 0);
        // and set the current format:
        this.stringFormats.add(defaultSF);
    }

    /**
     * Get the paragraph format. 
     * 
     * @return The paragraph format.
     */
    public ParagraphFormat getParagraphFormat() {
        return this.paragraphFormat;
    }

    /**
     * Get the string format of the glyph specified by the index. <br>
     * <b>NOTE</b>: Even if this glyph is not a Char type, it will still 
     * return a valid string format object. Just ignore the result returned. 
     * 
     * @param index The index of the glyph.
     * @return The string format of the glyph.
     */
    public StringFormat getStringFormat(int index) {
        Assert.checkRange(index, glyphs);

        Iterator it = stringFormats.iterator();
        while(it.hasNext()) {
            StringFormat sf = (StringFormat)it.next();
            if(sf.contains(index))
                return sf;
        }
        // should not reach here:
        throw new RuntimeException("StringFormats in this paragraph is invalid.");
    }

    public void resetStringFormats(ArrayList<StringFormat> sf) {
    	this.stringFormats = sf;
    }
    
    /**
     * To use new Font to format the string which is part of the 
     * paragraph. This method may create new StringFormat or break 
     * the existing StringFormat. 
     * 
     * @param startIndex The start index of the string.
     * @param endIndex The end index of the string.
     * @param fontName The font name, or null if ignore.
     * @param fontSize The font size, or null if ignore.
     * @param bold The bold attribute, or null if ignore.
     * @param italic The italic attribute, or null if ignore.
     * @param underlined The underlined attribute, or null if ignore.
     * @param color The color, or null if ignore.
     */
    public ArrayList<StringFormat> format(int startIndex, int endIndex, String fontName, Integer fontSize, Boolean bold, Boolean italic, Boolean underlined, yColor color) {
    	ArrayList<StringFormat> previousSF = new ArrayList<StringFormat>();
    	Iterator it = stringFormats.iterator();
    	while(it.hasNext()) {
    		StringFormat sf = (StringFormat)it.next();
    		previousSF.add(new StringFormat(sf));
    	}
    	setFormatted(false);
        if(startIndex>endIndex) {
            // swap:
            int temp = endIndex;
            endIndex = startIndex;
            startIndex = temp;
        }
        // include '\r' if:
        if( endIndex == (this.getGlyphsCount()-2) )
            endIndex++;

        int start=0;
        int end=0;
        boolean start_ok = false;
        boolean end_ok = false;

        Iterator it1 = this.stringFormats.iterator();
        int loop = 0;
        while(it1.hasNext()) {
            StringFormat sf = (StringFormat)it1.next();
            if(sf.contains(startIndex)) {
                // mark this StringFormat as start!
                start = loop;
                if(sf.getStartIndex()==startIndex) start_ok = true;
            }
            if(sf.contains(endIndex)) {
                end = loop;
                if(sf.getEndIndex()==endIndex) end_ok = true;
                break;
            }
            loop++;
        }

        // we must consider one special situation that one string format 
        // may be divied into 2 or 3 parts:
        if(start==end) {
            StringFormat current = (StringFormat)stringFormats.get(start);
            if(!start_ok && !end_ok) {
                // to 3 parts:
                int s1 = current.getStartIndex();
                int e1 = startIndex - 1;
                int s2 = startIndex;
                int e2 = endIndex;
                int s3 = endIndex + 1;
                int e3 = current.getEndIndex();
                // modify current:
                current.setEndIndex(startIndex - 1); // e1
                // insert 2 new string format:
                stringFormats.add(start+1,
                    new StringFormat(
                        this,
                        yFontFactory.instance().createFont(
                            fontName==null?current.getFont().getName():fontName,
                            fontSize==null?current.getFont().getSize():fontSize.intValue(),
                            bold==null?current.getFont().getBold():bold.booleanValue(),
                            italic==null?current.getFont().getItalic():italic.booleanValue(),
                            underlined==null?current.getFont().getUnderlined():underlined.booleanValue()
                         ),
                         color==null?current.getColor():color,
                         startIndex,
                         endIndex
                     )
                );
                stringFormats.add(start+2,
                    new StringFormat(
                        this,
                        yFontFactory.instance().createFont(
                            current.getFont().getName(),
                            current.getFont().getSize(),
                            current.getFont().getBold(),
                            current.getFont().getItalic(),
                            current.getFont().getUnderlined()
                        ),
                    current.getColor(),
                    endIndex+1,
                    e3)
                );
                return previousSF;
            }
            if(end_ok && !start_ok) {
                // just break the start part:
                int s1 = current.getStartIndex();
                int e1 = startIndex - 1;
                int s2 = startIndex;
                int e2 = endIndex; // = current.getEndIndex() as well
                // modify current:
                current.setEndIndex(e1);
                // insert a new one before current:
                stringFormats.add(start+1,
                    new StringFormat(
                        this,
                        yFontFactory.instance().createFont(
                            fontName==null?current.getFont().getName():fontName,
                            fontSize==null?current.getFont().getSize():fontSize.intValue(),
                            bold==null?current.getFont().getBold():bold.booleanValue(),
                            italic==null?current.getFont().getItalic():italic.booleanValue(),
                            underlined==null?current.getFont().getUnderlined():underlined.booleanValue()
                        ),
                        color==null?current.getColor():color,
                        s2, e2
                    )
                );
                return previousSF;
            }
            if(start_ok && !end_ok) {
                // just break the end part:
                int s1 = startIndex; // = current.getStartIndex() as well
                int e1 = endIndex;
                int s2 = endIndex + 1;
                int e2 = current.getEndIndex();
                // modify current:
                current.setStartIndex(s2);
                // insert a new one after it:
                stringFormats.add(start,
                    new StringFormat(
                        this,
                        yFontFactory.instance().createFont(
                            fontName==null?current.getFont().getName():fontName,
                            fontSize==null?current.getFont().getSize():fontSize.intValue(),
                            bold==null?current.getFont().getBold():bold.booleanValue(),
                            italic==null?current.getFont().getItalic():italic.booleanValue(),
                            underlined==null?current.getFont().getUnderlined():underlined.booleanValue()
                        ),
                        color==null?current.getColor():color,
                        startIndex, endIndex
                    )
                );
                return previousSF;
            }
            // ok, just modify:
            current.setFont(yFontFactory.instance().createFont(
                fontName==null?current.getFont().getName():fontName,
                fontSize==null?current.getFont().getSize():fontSize.intValue(),
                bold==null?current.getFont().getBold():bold.booleanValue(),
                italic==null?current.getFont().getItalic():italic.booleanValue(),
                underlined==null?current.getFont().getUnderlined():underlined.booleanValue()
            ));
            current.setColor(color==null?current.getColor():color);
            return previousSF;
        }

        int formatFrom = start;
        if(!start_ok) {
            // StringFormat[start] should be broke,
            StringFormat cur = (StringFormat) stringFormats.get(start);
            // add a new StringFormat:
            stringFormats.add(start+1, new StringFormat(this, cur.getFont(), cur.getColor(), startIndex, cur.getEndIndex()));
            // and modify its end index:
            cur.setEndIndex(startIndex-1);
            // delete from the next string format:
            formatFrom = start + 1;
            end++;
        }

        int formatEnd = end;
        if(!end_ok) {
            // StringFormat[end] should be broke,
            StringFormat cur = (StringFormat)stringFormats.get(end);
            // add a new StringFormat:
            stringFormats.add(end, new StringFormat(this, cur.getFont(), cur.getColor(), cur.getStartIndex(), endIndex));
            // modify its start index:
            cur.setStartIndex(endIndex+1);
            formatEnd = end;
        }

        boolean bChangeFont = fontName!=null || fontSize!=null || bold!=null || italic!=null || underlined!=null;
        // now we must set new format from [formatFrom, formatEnd]:
        for(int i=formatFrom; i<=formatEnd; i++) {
            StringFormat sf = (StringFormat)stringFormats.get(i);
            if(color!=null) {
                // change its color:
                sf.setColor(color);
            }
            if(bChangeFont) {
                // font must changed:
                yFont font = yFontFactory.instance().createFont(
                    fontName==null?sf.getFont().getName():fontName,
                    fontSize==null?sf.getFont().getSize():fontSize.intValue(),
                    bold==null?sf.getFont().getBold():bold.booleanValue(),
                    italic==null?sf.getFont().getItalic():italic.booleanValue(),
                    underlined==null?sf.getFont().getUnderlined():underlined.booleanValue()
                );
                sf.setFont(font);
            }
        }
        this.debug();
        return previousSF;
//        StringFormat current = (StringFormat)stringFormats.get(deleteFrom);
        // now first we delete all between [deleteFrom, deleteEnd]:
//        for(int i=deleteEnd; i>=deleteFrom; i--)
//            stringFormats.remove(i);
//         then insert a new string format into deleteFrom:
//        stringFormats.add(deleteFrom, new StringFormat(this, 
//            FontFactory.instance().createFont(
//                fontName==null?current.getFont().getName():fontName,
//                fontSize==null?current.getFont().getSize():fontSize.intValue(),
//                bold==null?current.getFont().getBold():bold.booleanValue(),
//                italic==null?current.getFont().getItalic():italic.booleanValue(),
//                underlined==null?current.getFont().getUnderlined():underlined.booleanValue()
//            ),
//            color==null?current.getColor():color,
//            startIndex, endIndex)
//        );
    }

    /**
     * The paragraph is formatted or not. If it returns "false", 
     * the paragraph will not layout properly on the screen before 
     * invoking ParagraphCompositor.compose() method. 
     */
    public boolean getFormatted() {
        return this.formatted;
    }

    /**
     * Set this paragraph formatted or not. 
     */
    public void setFormatted(boolean formatted)
    {
        this.formatted = formatted;
    }

    /**
     * Combine the two paragraphs into one, the second paragraph (which passed 
     * as a parameter) should be removed from the document after combination. 
     * 
     * @param next The paragraph to be combined with.
     */
    public void combine(Paragraph next, List<StringFormat> preStringFormats) {
        int inc_sf = this.getGlyphsCount()-1;
        preStringFormats.addAll(this.stringFormats);
        
        // if the current paragraph is empty (only have '\r'):
        if(this.getGlyphsCount()==1) {
            // clear it:
            this.glyphs.clear();
            this.stringFormats.clear();
        }
        else {
            // we remove the '\r' in the current paragraph:
            this.glyphs.remove(glyphs.size()-1);
            StringFormat last_sf = (StringFormat)this.stringFormats.get(stringFormats.size()-1);
            if(last_sf.getEndIndex()==last_sf.getStartIndex())
                this.stringFormats.remove(last_sf);
            else
                last_sf.increase(-1);
        }
        // then copy all contents from p:
        Iterator g_it = next.glyphs.iterator();
        while(g_it.hasNext())
            this.glyphs.add(g_it.next());
        // and all stringFormats:
        Iterator sf_it = next.stringFormats.iterator();
        while(sf_it.hasNext()) {
            StringFormat sf = (StringFormat)sf_it.next();
            StringFormat new_sf = new StringFormat(
                this, sf.getFont(), sf.getColor(),
                sf.getStartIndex() + inc_sf, sf.getEndIndex() + inc_sf
            );
            this.stringFormats.add(new_sf);
        }
        setFormatted(false);
    }

    /**
     * decompose the specific part of a paragraph, the part (which passed 
     * as a parameter) should be encapsulated as a paragraph. 
     * 
     * @param para The specific part of a paragraph to be removed.
     */
    public void decompose(Paragraph para, List preStringFormats) {
        int inc_sf = this.getGlyphsCount()-1;
        int removedSize = para.getGlyphsCount() - 1;
        
        if(inc_sf < removedSize) {
        	return;
        }
        
        this.stringFormats = preStringFormats;
        
        // if the current paragraph is empty (only have '\r'):
        if(this.getGlyphsCount()==1) {
            // clear it:
            this.glyphs.clear();
            this.stringFormats.clear();
        }
        else {
            // we remove the '\r' in the current paragraph:
            this.glyphs.remove(glyphs.size()-1);
            StringFormat last_sf = (StringFormat)this.stringFormats.get(stringFormats.size()-1);
            if(last_sf.getEndIndex()==last_sf.getStartIndex())
                this.stringFormats.remove(last_sf);
            else
                last_sf.increase(-1);
        }
        // then remove all contents of p:
        int numGlyphs = para.glyphs.size() - 1;
    	int indexGlyphs = stringFormats.size() - 1;
        while(numGlyphs != 0) {
        	this.glyphs.remove(indexGlyphs);
        	indexGlyphs--;
        	numGlyphs--;
        }
        // and remove all stringFormats:
        int numSF = para.stringFormats.size() - 1;
    	int indexSF = stringFormats.size() - 1;
        while(numSF != 0) {
        	this.stringFormats.remove(indexSF);
        	indexSF--;
        	numSF--;
        }
        setFormatted(false);
    }
    /**
     * To check whether string format is changed
     * 
     * @author y&y
     * @param fontname the font used to check whether current string format is changed
     */
    public boolean isStringFormatChanged(String fontname) {
    	return currentStringFormat.getFont().getName().equals(fontname) ? false : true; 
    }
    
    /**
     * change the current string format
     * @author y&y
     * 
     * @param fontName The font name, or null if ignore.
     * @param fontSize The font size, or null if ignore.
     * @param bold The bold attribute, or null if ignore.
     * @param italic The italic attribute, or null if ignore.
     * @param underlined The underlined attribute, or null if ignore.
     * @param color The color, or null if ignore.
     */
    
    public StringFormat changeCurrentStringFormat(String fontName, Integer fontSize, Boolean bold, Boolean italic, Boolean underlined, yColor color) {
    	StringFormat tmp = new StringFormat(currentStringFormat);
    	currentStringFormat.setFont(yFontFactory.instance().createFont(
    					fontName==null?currentStringFormat.getFont().getName():fontName,
    					fontSize==null?currentStringFormat.getFont().getSize():fontSize.intValue(),
    					bold==null?currentStringFormat.getFont().getBold():bold.booleanValue(),
    					italic==null?currentStringFormat.getFont().getItalic():italic.booleanValue(),
    					underlined==null?currentStringFormat.getFont().getUnderlined():underlined.booleanValue()
    			));
    	currentStringFormat.setColor(color==null?currentStringFormat.getColor():color);
//    	isChanged = true;
    	return tmp;
    }
    
	/**
	 * Add a glyph to its child list, must be "leaf" glyph such as 
	 * Char or Picture.<br>
	 * Usually a ParagraphCompositor.compose() should be called 
	 * after one or more times of calling add() or remove().
	 * 
	 * @param index The position to be added.
	 * @param g The glyph object to be added.
	 */
	public void add(int index, Glyph g) {
//		Assert.checkNull(g);
//		Assert.checkTrue(index>=0 && index<glyphs.size()); // MUST before '\r'!
//		Assert.checkTrue(g instanceof Char || g instanceof Picture);
//		Assert.checkTrue(!g.equals(Char.RETURN));
//
//		// first add the glyph:
//		this.glyphs.add(index, g);
//		
//		// check whether current stringFormat is change
//		// if so, insert new stringFormat into stringFormats
//		if(isChanged) {
//			yFont font = currentStringFormat.getFont();
//			yColor color = currentStringFormat.getColor();
//			format(index, index, font.getName(), font.getSize(),
//					font.getBold(), font.getItalic(), font.getUnderlined(), color);
//			isChanged = false;
//			setFormatted(false);
//			return;
//		}
//		
//		// update the string formats:
//		Iterator it = stringFormats.iterator();
//		boolean after = false;
//		while(it.hasNext()) {
//		    StringFormat sf = (StringFormat) it.next();
//
//		    if(sf.contains(index)) {
//		        // Now this StringFormat should increase:
//		        sf.increase(1);
//		        after = true;
//		    }
//		    else {
//		        if(after) sf.move(1);
//		    }
//		}
//		// set to unformatted:
//		setFormatted(false);

		Assert.checkNull(g);
		Assert.checkTrue(index>=0 && index<glyphs.size()); // MUST before '\r'!
		Assert.checkTrue(g instanceof Char || g instanceof Picture);
		Assert.checkTrue(!g.equals(Char.RETURN));

		// first add the glyph:
		this.glyphs.add(index, g);
		// update the string formats:
		Iterator it = stringFormats.iterator();
		boolean after = false;
		while(it.hasNext()) {
		    StringFormat sf = (StringFormat) it.next();

		    if(sf.contains(index)) {
		        // Now this StringFormat should increase:
		        sf.increase(1);
		        after = true;
		    }
		    else {
		        if(after) sf.move(1);
		    }
		}
		// set to unformatted:
		setFormatted(false);
    
	}

	/**
	 * used to recover from deletecommand
	 * 
	 * @param item the deletedItem deleted by previous delete action
	 */
	public void addDeletedItem(DeletedItem item) {
		if(item == null) {
			return;
		}
		add(item.indexOfDeletedGlyph, item.deletedGryph);
		if(item.hasDeletedStringFormat) {
			stringFormats.add(item.indexOfDeletedSF, item.deletedStringFormat);
		}
	}
	
    /**
     * Append a glyph to its child list to the last position. 
     * In fact it will inserted before '\r'.
     * 
     * @param g The glyph object to be added.
     */
    public void add(Glyph g) {
        // call another add():
        add(glyphs.size()-1, g); // NOTE: Must insert before '\r'!
    }

    /**
     * Insert a character at the specified position of the paragraph.
     * 
     * @param index The position of the paragraph.
     * @param c The char code.
     */
    public void add(int index, char c) {
        // call another add():
        add(index, CharFactory.instance().createChar(c));
    }

    /**
     * Append a character at the end of the paragraph.
     * 
     * @param c The char code.
     */
    public void add(char c) {
        // call another add():
        add(CharFactory.instance().createChar(c));
    }

	/**
	 * Get the specified child glyph.
	 * 
	 * @param index The child position.
	 * @return The specified child glyph.
	 */
	public Glyph child(int index) {
		Assert.checkRange(index, glyphs);
		return (Glyph) glyphs.get(index);
	}

    /**
     * Remove a glyph from the paragraph. 
     * 
     * @param index The index of the glyph.
     */
    public DeletedItem remove(int index) {
        Assert.checkTrue(index>=0 && index<(getGlyphsCount()-1));

        DeletedItem item = new DeletedItem();
        item.indexOfDeletedGlyph = index;
        item.deletedGryph = (Glyph) glyphs.get(index); 
        glyphs.remove(index);
        // NOTE: synchronize StringFormats:
        Iterator it = stringFormats.iterator();
        boolean after = false;
        int indexOfSF = -1;
        while(it.hasNext()) {
            StringFormat sf = (StringFormat)it.next();
            indexOfSF++;
            if(after) {
                sf.move(-1); // move previous!
            }
            else {
                if (sf.contains(index)) {
                    if(sf.getStartIndex()==sf.getEndIndex()) {
                        item.deletedStringFormat = sf;
                        item.indexOfDeletedSF = indexOfSF;
                        item.hasDeletedStringFormat = true;
                        it.remove(); // this string format should be delete!
                    }
                    else {
                        sf.increase(-1); // ok, this format should decrease!
                        item.hasDeletedStringFormat = false;
                    }
                    after = true;
                }
            }
        }
        setFormatted(false);
        return item;
    }
    
    /**
     * Remove a range of glyphs specified by [startIndex, endIndex]. 
     * 
     * @param startIndex The start index.
     * @param endIndex The end index.
     */
    public void removeGlyphs(int startIndex, int endIndex) {
        for(int i=endIndex; i>=startIndex; i--) {
            remove(i);
        }
    }

	/**
	 * Get the count of the child glyphs.
	 * 
	 * @return The count of the child glyphs.
	 */
	public int getGlyphsCount() {
		return glyphs.size();
	}

	/**
	 * Break this paragraph into 2 paragraphs. The first one is this 
	 * paragraph itself but is been modified. The second one is returned 
	 * and should be inserted into the document. 
	 * 
	 * @param index_of_from Where to break this paragraph.
	 * @return The second paragraph.
	 */
    public Paragraph split(int index_of_from) {
        Assert.checkTrue(index_of_from>=0 && index_of_from<getGlyphsCount());

        // first copy all glyphs and stringFormats to the second paragraph:
        Paragraph p2 = this.copy();

        // for this paragraph, remove [index_of_from, end):
        this.removeGlyphs(index_of_from, getGlyphsCount()-2);
        this.fix();

        // for p2, remove the [0, index_of_from):
        p2.removeGlyphs(0, index_of_from-1);
        this.setFormatted(false);
        p2.setFormatted(false);
        return p2;
    }

    // make the last '\r' the same format with the previous one:
    private void fix() {
        int size = stringFormats.size();
        if(size>1) {
            StringFormat last = (StringFormat)stringFormats.get(size-1);
            if(last.getStartIndex()==last.getEndIndex()) {
                StringFormat pre = (StringFormat)stringFormats.get(size-2);
                pre.increase(1);
                stringFormats.remove(size-1);
            }
        }
    }

    // return the same paragraph object:
    private Paragraph copy() {
        Paragraph p = new Paragraph(this.document);
        p.formatted = this.formatted;
        p.paragraphFormat = this.paragraphFormat;

        // copy glyphs:
        p.glyphs.clear();
        Iterator g_it = this.glyphs.iterator();
        while(g_it.hasNext()) {
            p.glyphs.add(g_it.next());
        }
        // copy string formats:
        p.stringFormats.clear();
        Iterator sf_it = this.stringFormats.iterator();
        while(sf_it.hasNext()) {
            StringFormat sf = (StringFormat)sf_it.next();
            p.stringFormats.add(new StringFormat(
                p, sf.getFont(), sf.getColor(), sf.getStartIndex(), sf.getEndIndex()
            ));
        }
        return p;
    }

    /**
     * This method is called by ParagraphCompositor to reset all rows. 
     * (To make all rows unavailable) 
     */
    public void clearAllRows() {
        this.rows.clear();
    }

    /**
     * This method is called by ParagraphCompositor to append a Row.
     *
     * @param row The row object to append.
     */
    public void appendRow(Row row) {
        Assert.checkNull(row);
        this.rows.add(row);
    }

    /**
     * Once the paragraph was formatted by a compositor, 
     * view can get the formatted rows to display.
     */
    public List getRows() {
        Assert.checkTrue(getFormatted());
        return this.rows;
    }

    /**
     * Get the count of the rows. 
     * 
     * @return How many rows in this paragraph.
     */
    public int getRowsCount() {
        return this.rows.size();
    }

    /**
     * Get the specified row. 
     * 
     * @param index The index of the row.
     * @return The row object.
     */
    public Row getRow(int index) {
        return (Row)this.rows.get(index);
    }

    /**
     * Get the index of the row. 
     * 
     * @param row The row object.
     * @return The index of the row, or (-1) if not found.
     */
    public int getRowIndex(Row row) {
        return this.rows.indexOf(row);
    }

    /**
     * Get the row space. 
     * 
     * @return The row space.
     */
    public int getRowSpace() {
        return this.paragraphFormat.getRowSpace();
    }

    /**
     * Get the available width for layout rows. <br>
     * <b>NOTE</b>: the scale width of the first row may 
     * be less because a "firstIndent" may not be 
     * ZERO. 
     * 
     * @param firstRow If this is the first row.
     * @return The available width.
     */
    public float scaleWidth(boolean firstRow) {
        return this.paragraphFormat.scaleWidth(firstRow);
    }

    /**
     * Get the available width for layout rows. <br>
     * <b>NOTE</b>: the scale width of the first row may 
     * be less because a "firstIndent" may not be 
     * ZERO. 
     * 
     * @param row The current row.
     * @return The available width.
     */
    public int scaleWidth(Row row) {
        Assert.checkNull(row);
        Assert.checkTrue(this.rows.size()>0);

        boolean firstRow = (this.rows.get(0)==row);
        return this.paragraphFormat.scaleWidth(firstRow);
    }

    /**
     * Get the total width of the paragraph.
     * 
     * @see y.module.Glyph#width()
     */
    public int width() {
        return this.paragraphFormat.getWidth();
    }

    /**
     * Unsupported operation.
     * 
     * @see y.module.Glyph#draw(y.module.yGraphics.Graphics)
     */
    public void draw(yGraphics g) {
        throw new UnsupportedOperationException("Paragraph shouldn never call draw().");
    }

    /**
     * Check if this row is the first row. 
     * 
     * @param row The row to be tested.
     * @return True if this row is the first row.
     */
    public boolean isFirstRow(Row row) {
        return this.rows.get(0)==row;
    }

    // some debug info:
    public void debug() {
        System.out.println("\n-- Paragraph info --");
        System.out.println("  total " + this.glyphs.size() + " glyphs:");
        StringBuffer sb = new StringBuffer(100);
        Iterator it = this.glyphs.iterator();
        while(it.hasNext()) {
            Object g = it.next();
            sb = sb.append(g.toString());
        }
        System.out.println(sb.toString());

        // show Rows:
        if(getFormatted()) {
            System.out.println("<formated> " + rows.size() + " rows:");
            Iterator it2 = this.rows.iterator();
            while(it2.hasNext()) {
                Row row = (Row)it2.next();
                row.debug();
            }
        }
        else {
            System.out.println("<unformated>");
        }
        
        // show StringFormat:
        System.out.print("<string formats>");
        Iterator it3 = this.stringFormats.iterator();
        while(it3.hasNext()) {
            StringFormat sf = (StringFormat)it3.next();
            System.out.print("  " + sf.toString());
        }
        System.out.println("\n-- End paragraph info --\n");
    }

    /* (non-Javadoc)
     * @see jexi.core.Glyph#intersects(int, int)
     */
    public boolean intersects(int x, int y) {
		throw new UnsupportedOperationException("Paragraph does not support intersects().");
    }

//	@Override
//	public void readExternal(ObjectInput in) throws IOException,
//			ClassNotFoundException {
//		this.document = (Document) in.readObject();
//		this.glyphs = (List) in.readObject();
//		this.stringFormats = (List) in.readObject();
//		this.rows = (List) in.readObject();
//		this.paragraphFormat = (ParagraphFormat) in.readObject();
//	}
//
//	@Override
//	public void writeExternal(ObjectOutput out) throws IOException {
//		out.writeObject(document);
//		out.writeObject(glyphs);
//		out.writeObject(stringFormats);
//		out.writeObject(rows);
//		out.writeObject(paragraphFormat);
//		
//	}
	
//    private void writeObject(ObjectOutputStream stream) throws IOException {
////    	stream.writeObject(glyphs);
//    	Iterator it = glyphs.iterator();
//    	while(it.hasNext()) {
//    		Glyph g = (Glyph)it.next();
//    		stream.writeObject(g);
//    	}
////    	stream.writeObject(stringFormats);
//    	it = stringFormats.iterator();
//    	while(it.hasNext()) {
//    		StringFormat g = (StringFormat)it.next();
//    		stream.writeObject(g);
//    	}
////    	stream.writeObject(rows);
//    	it = rows.iterator();
//    	while(it.hasNext()) {
//    		Row g = (Row)it.next();
//    		stream.writeObject(g);
//    	}
//    	stream.writeObject(paragraphFormat);
//    	stream.writeObject(formatted);
//    }
//    
//    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
////    	this.glyphs = (List)stream.readObject();
////    	this.stringFormats = (List)stream.readObject();
////    	this.rows = (List)stream.readObject();
//    	this.paragraphFormat = (ParagraphFormat)stream.readObject();
//    	this.formatted = (Boolean)stream.readObject();
//    }
}
