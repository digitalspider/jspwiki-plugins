package com.digitalspider.jspwiki.plugin;

import java.util.Collection;

import junit.framework.TestCase;

import org.apache.commons.validator.GenericValidator;

public class ImageGalleryPluginTest extends TestCase {

	String VALID_URL = "http://smh.com.au";
	String INVALID_URL = "http://smhfsdfwerwrsdfsdf";
	ImageGalleryPlugin plugin = new ImageGalleryPlugin();

	private static final String HTML_DATA = 
			"    <article class='article feature hero-portrait clippingArea clippingAction' data-assetId='d-109sjh' data-assetType='ARTICLE' data-assetUrl='http://www.smh.com.au/world/lady-alqaeda-the-useducated-phd-the-islamic-state-desperately-wants-to-free-20140829-109sjh.html'>"+
			"        <!-- javascript will insert <button class='clipping' title='Save [headline]'>Add to my clippings</button> here, by using data attributes attached to article tag above.-->"+
			"                        <a href='http://www.smh.com.au/world/lady-alqaeda-the-useducated-phd-the-islamic-state-desperately-wants-to-free-20140829-109sjh.html' title=''Lady al-Qaeda': The US-educated PhD the Islamic State desperately wants to free'><img src='http://images.smh.com.au/2014/08/29/5718893/ladyalqaeda-300x370.jpg' width='300' height='370' alt='Dr Aafia Siddiqui' /></a>"+
			"            <div class='wof'>                "+
			"        <h3><a href='http://www.smh.com.au/world/lady-alqaeda-the-useducated-phd-the-islamic-state-desperately-wants-to-free-20140829-109sjh.html' title='Militants want their lady back'>Militants want their lady back</a></h3>        "+
			"                <p>She was a brilliant neuroscientist and mother, who apparently cast off comfort and success to  became the most wanted woman in the world.</p>"+
			"            </div>"+
			"    </article><!-- class:article -->"+
			"                </div>"+
			"                <div class='col last'>"+
			"    <article class='article feature' >"+
			"        <!-- javascript will insert <button class='clipping' title='Save [headline]'>Add to my clippings</button> here, by using data attributes attached to article tag above.-->"+
			"            <span class='kicker'>smh.tv</span>"+
			"                        <a href='Rom-com starring David Boreanaz. Lance is Mr Fix It, the ultimate player, until he meets his dream girl.' title='Mr Fix It'><img src='http://images.smh.com.au/2014/08/29/5716645/fix_st-300x0.jpg' width='300' alt='Fix' /></a>"+
			"            <div class='wof'>                "+
			"        <h3><a href='Rom-com starring David Boreanaz. Lance is Mr Fix It, the ultimate player, until he meets his dream girl.' title='Mr Fix It'>Mr Fix It</a></h3>        "+
			"                <p>Rom-com starring David Boreanaz. Lance is Mr Fix It, the ultimate player, until he meets his dream girl.</p>                "+
			"            </div>"+
			"    </article><!-- class:article -->";
	
    public void setUp() throws Exception {

    }
    
	public void tearDown() throws Exception {
		
	}
	
	public void testUrlValidator() {
		assertTrue(VALID_URL+" should have been a valid URL", GenericValidator.isUrl(VALID_URL));
		assertFalse(INVALID_URL+" should have been an invalid URL", GenericValidator.isUrl(INVALID_URL));
	}
	
	public void testFindImages() {
		Collection<String> images = ImageGalleryPlugin.findImages(HTML_DATA);
		System.out.println("images="+images);
		assertEquals("Did not find 2 images in HTML data", 2, images.size());
		assertEquals("Image1 is not valid", "http://images.smh.com.au/2014/08/29/5716645/fix_st-300x0.jpg", images.iterator().next());
		assertEquals("Image2 is not valid", "http://images.smh.com.au/2014/08/29/5718893/ladyalqaeda-300x370.jpg", images.toArray()[1]);
	}
	
	public void testPlainTextValidation() {
		String value = ImageGalleryPlugin.findFirstByRegex("garfield",ImageGalleryPlugin.REGEX_PLAINTEXT);
		assertEquals("garfield", value);
		assertEquals("dilbert-classics", ImageGalleryPlugin.findFirstByRegex("dilbert-classics",ImageGalleryPlugin.REGEX_PLAINTEXT));
		assertEquals("dil", ImageGalleryPlugin.findFirstByRegex("dil//bert-classics",ImageGalleryPlugin.REGEX_PLAINTEXT));
		assertEquals("dilbert", ImageGalleryPlugin.findFirstByRegex("dilbert?classics",ImageGalleryPlugin.REGEX_PLAINTEXT));
		assertEquals("di", ImageGalleryPlugin.findFirstByRegex("di&lbert-classics",ImageGalleryPlugin.REGEX_PLAINTEXT));
	}
}
