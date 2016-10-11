import java.util.ArrayList;

public class Vessel {

	private ArrayList<Container>[] columns;
	public ArrayList<Container>[] getColumns() {
		return columns;
	}

	@SuppressWarnings("unchecked")
	public Vessel(int col) {
		columns = new ArrayList[col];
		for (int i = 0; i < col; i++)
			columns[i] = new ArrayList<Container>();
	}

	public void addContainer(Container c, int indexCol) {
		columns[indexCol].add(c);
	}

}
