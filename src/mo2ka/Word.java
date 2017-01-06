package mo2ka;

import java.text.ParseException;

public class Word{

	private String word;
	private String type1;
	private String type2;
	private String type3;
	private String type4;
	private String type5;
	private String type6;

	public Word(String word, String type1, String type2, String type3, String type4, String type5, String type6){
		this.word = word;
		this.type1 = type1;
		this.type2 = type2;
		this.type3 = type3;
		this.type4 = type4;
		this.type5 = type5;
		this.type6 = type6;
	}

	public Word(String word, String hyphenList) throws ParseException{
		this.word = word;
		String[] types = hyphenList.split("-");
		if(types.length < 6)
			throw new ParseException("Wrong mecab feature size", -1);
		this.type1 = types[0];
		this.type2 = types[1];
		this.type3 = types[2];
		this.type4 = types[3];
		this.type5 = types[4];
		this.type6 = types[5];
	}

	public String getWord(){
		return word;
	}

	public String[] getAllTypes(){
		return new String[]{type1, type2, type3, type4, type5, type6};
	}

	public String[] getTypes(int start, int length){
		String[] types = getAllTypes();
		String[] result = new String[length];
		int count = 0;
		for(int i = start; i < start + length; i++){
			result[count] = types[i];
			count++;
		}
		return result;
	}

	public String getType1(){
		return type1;
	}

	public String getType2(){
		return type2;
	}

	public String getType3(){
		return type3;
	}

	public String getType4(){
		return type4;
	}

	public String getType5(){
		return type5;
	}

	public String getType6(){
		return type6;
	}

}
