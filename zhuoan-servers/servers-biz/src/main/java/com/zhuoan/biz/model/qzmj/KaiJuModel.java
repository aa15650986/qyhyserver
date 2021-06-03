
package com.zhuoan.biz.model.qzmj;

public class KaiJuModel {
	private int index;
	private int type;
	private int[] values;
	private int showType;

	public KaiJuModel() {
	}

	public KaiJuModel(int index, int type, int[] values) {
		this.index = index;
		this.type = type;
		this.values = values;
		this.showType = 0;
	}

	public int getIndex() {
		return this.index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getType() {
		return this.type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int[] getValues() {
		return this.values;
	}

	public void setValues(int[] values) {
		this.values = values;
	}

	public int getShowType() {
		return this.showType;
	}

	public void setShowType(int showType) {
		this.showType = showType;
	}
}
