package tr.com.logidex.cad;

public class LabelDesigUtil {



	public static String addNewLineIfTooLong(String s, int charCountLimit) {
		if (charCountLimit < 5){
			throw new IllegalArgumentException("charCountLimit must be greater than or equal to 5");
		}

		// sag ve sol bosluklari temizle, bos satirlar varsa sil.
	  s =  s.trim().replaceAll("(?m)^[ \t]*\r?\n", "");
	
	StringBuilder sb = new StringBuilder();

		for (int i = 0; i < s.length(); i++) {
		    sb.append(s.charAt(i));
		    if ((i + 1) % charCountLimit == 0) {
		        sb.append("\n");
		    }
		}

		return sb.toString();

	}


	public static String[] splitStringIntoChunks(String input,int chunkSize) {


		// Girdi dizisinin uzunluğu
		int inputLength = input.length();

		// Dizi boyutunu belirlemek için gereken parça sayısını hesapla
		int numOfChunks = (int) Math.ceil((double) inputLength / chunkSize);

		// Sonuç dizisini oluştur
		String[] chunks = new String[numOfChunks];

		// Girdi dizisini parçalara böl
		for (int i = 0; i < numOfChunks; i++) {
			int start = i * chunkSize;
			int end = Math.min(inputLength, start + chunkSize);
			chunks[i] = input.substring(start, end);
		}

		return chunks;
	}
}
