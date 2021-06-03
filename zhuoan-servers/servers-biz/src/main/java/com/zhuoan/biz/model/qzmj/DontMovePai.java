
package com.zhuoan.biz.model.qzmj;

import java.util.ArrayList;
import java.util.List;

public class DontMovePai {
	private int type;
	private List<Integer> pai;
	private int foucsPai;

	public DontMovePai() {
	}

	public DontMovePai(int type, int[] pai, int foucsPai) {
		this.type = type;
		this.pai = new ArrayList();

		for(int i = 0; i < pai.length; ++i) {
			this.pai.add(pai[i]);
		}

		this.foucsPai = foucsPai;
	}

	public void updateDontMovePai(int type, int pai, int foucsPai) {
		this.type = type;
		this.pai.add(pai);
		this.foucsPai = foucsPai;
	}

	public int getType() {
		return this.type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int[] getPai() {
		int[] newpai = new int[this.pai.size()];

		for(int i = 0; i < this.pai.size(); ++i) {
			newpai[i] = (Integer)this.pai.get(i);
		}

		return newpai;
	}

	public void setPai(int[] pai) {
		this.pai = new ArrayList();

		for(int i = 0; i < pai.length; ++i) {
			this.pai.add(pai[i]);
		}

	}

	public int getFoucsPai() {
		return this.foucsPai;
	}

	public void setFoucsPai(int foucsPai) {
		this.foucsPai = foucsPai;
	}
}
