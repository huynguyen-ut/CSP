import java.util.ArrayList;

public class Container {

	private int index; // so thu tu

	public int getIndex() {
		return index;
	}

	private int t1; // thoi diem boc vao bai tam hoac bai chinh

	public void setT1(int t1) {
		this.t1 = t1;
	}

	public int getT1() {
		return t1;
	}

	private int t2 = -1; // thoi diem boc tu bai tam vao bai chinh, -1 la ko boc

	public void setT2(int t2) {
		this.t2 = t2;
	}

	public int getT2() {
		return t2;
	}

	private ArrayList<Container> aboveContainers; // tap cac Container nam o tren (phai boc truoc)

	public ArrayList<Container> getAboveContainers() {
		return aboveContainers;
	}

	public void addAboveContainers(Container c) {
		this.aboveContainers.add(c);
	}

	public Container(int index) {
		this.index = index;
		this.aboveContainers = new ArrayList<Container>();
	}

}
