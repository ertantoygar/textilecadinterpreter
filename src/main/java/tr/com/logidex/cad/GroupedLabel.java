package tr.com.logidex.cad;


import tr.com.logidex.cad.model.Lbl;

import java.util.ArrayList;


/**
 *
 * Birden fazla "Lbl" dan olustan bir "LabelGroup" olusturur. Yaziciya
 * gonderilecek olan nihai etiket modeli.
 * 
 * @author Ertan
 */
public class GroupedLabel extends ArrayList<Lbl> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public boolean add(Lbl e) {
		return super.add(e);
	}



	@Override
	public Lbl get(int index) {
		return super.get(index);
	}

}
