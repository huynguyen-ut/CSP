import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class Model2 {

	private Vessel vessel;
	private Yard yard;
	private int nC; // so luong Container
	private int n; // so dong tren tau
	private int m; // so cot tren tau
	private int h; // so dong (chieu cao) bai chinh
	private int J; // so cot (chieu dai) bai chinh + 1 bai tam (index=0)
	private int T; // thoi diem Container duoc boc ra khoi tau, T = nC

	/* GUROBI */
	private double objVal, runtime, lowerBound;
	private GRBVar[][][] x, y, z;

	public Model2(String ifileName, String ofileName, String vfileName) throws Exception {
		readData(ifileName);
		printData();
		solve();
		printVariables(vfileName);
		saveData(ofileName);
	}

	private void readData(String fileName) throws Exception {
		Scanner inFile = new Scanner(new File(fileName));

		// doc cau hinh (dong dau tien)
		String s = inFile.nextLine();
		String[] tokens = s.split("\t");
		n = Integer.parseInt(tokens[0]);
		m = Integer.parseInt(tokens[1]);
		h = Integer.parseInt(tokens[2]);
		J = Integer.parseInt(tokens[3]) + 1;

		// doc danh sach Container (cac dong ben duoi)
		vessel = new Vessel(m);
		for (int i = 0; i < n; i++) {
			s = inFile.nextLine();
			tokens = s.split("\t");
			for (int j = 0; j < tokens.length; j++) {
				int index = Integer.parseInt(tokens[j]);
				if (index != -1) {
					Container c = new Container(index);
					for (Container above : vessel.getColumns()[j]) {
						c.addAboveContainers(above);
					}
					vessel.addContainer(c, j);
					nC++;
				}
			}
		}
		T = 2 * nC;
		inFile.close();
	}

	public void printData() {
		System.out.println("\nContainers: " + nC);
		for (int j = 0; j < vessel.getColumns().length; j++) {
			ArrayList<Container> lst = vessel.getColumns()[j];
			System.out.print("Col " + j + ": ");
			for (int i = lst.size() - 1; i >= 0; i--) {
				Container c = lst.get(i);
				System.out.print(c.getIndex() + "\t");
			}
			System.out.println();
		}
		System.out.println("------------------------------");
	}

	public void solve() throws Exception {
		// create Enviroment & Model
		GRBEnv env = new GRBEnv();
		GRBModel model = new GRBModel(env);

		// decision variables
		x = new GRBVar[this.nC + 1][J][T];
		y = new GRBVar[this.nC + 1][this.nC + 1][T];
		z = new GRBVar[this.nC + 1][J][T];
		for (int i = 1; i <= this.nC; i++)
			for (int j = 0; j < J; j++)
				for (int t = 0; t < T; t++) {
					x[i][j][t] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x_" + i + "_" + j + "_" + t);
					z[i][j][t] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "z_" + i + "_" + j + "_" + t);
				}
		for (int i = 1; i < this.nC; i++)
			for (int k = i + 1; k < this.nC + 1; k++)
				for (int t = 0; t < T; t++)
					y[i][k][t] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y_" + i + "_" + k + "_" + t);
		model.update();

		// constraints
		GRBLinExpr expr = null;
		// rang buoc 1: phu thuoc vào vi trí 4.2
		for (ArrayList<Container> lst : vessel.getColumns())
			for (Container i : lst)
				for (Container k : i.getAboveContainers()) {
					expr = new GRBLinExpr();
					for (int j = 0; j < J; j++)
						for (int t = 0; t < T; t++) {
							expr.addTerm(t, x[i.getIndex()][j][t]);
							expr.addTerm(-t, x[k.getIndex()][j][t]);
						}
					model.addConstr(expr, GRB.GREATER_EQUAL, 1, "contr4.2");
				}
		// rang buoc 2: Phu thuoc vao thu tu 4.4
		for (ArrayList<Container> lst1 : vessel.getColumns())
			for (Container i : lst1)
				for (ArrayList<Container> lst2 : vessel.getColumns())
					for (Container k : lst2)
						if (k.getIndex() > i.getIndex())
							for (int j = 1; j < J; j++) {
								expr = new GRBLinExpr();
								expr.addTerm(nC, y[i.getIndex()][k.getIndex()][j]);
								for (int t = 0; t < T; t++) {
									expr.addTerm(t, x[i.getIndex()][j][t]);
									expr.addTerm(t, z[i.getIndex()][j][t]);
									expr.addTerm(-t, x[k.getIndex()][j][t]);
									expr.addTerm(-t, z[k.getIndex()][j][t]);
								}
								model.addConstr(expr, GRB.GREATER_EQUAL, 1, "contr4.4");
							}
		for (ArrayList<Container> lst1 : vessel.getColumns())
			for (Container i : lst1)
				for (ArrayList<Container> lst2 : vessel.getColumns())
					for (Container k : lst2)
						if (k.getIndex() > i.getIndex())
							for (int j = 1; j < J; j++) {
								// rang buoc 4: 4.5
								expr = new GRBLinExpr();
								expr.addTerm(1.0, y[i.getIndex()][k.getIndex()][j]);
								for (int t = 0; t < T; t++) {
									expr.addTerm(0.5, x[i.getIndex()][j][t]);
									expr.addTerm(0.5, z[i.getIndex()][j][t]);
									expr.addTerm(0.5, x[k.getIndex()][j][t]);
									expr.addTerm(0.5, z[k.getIndex()][j][t]);
								}
								model.addConstr(expr, GRB.GREATER_EQUAL, 1, "contr4.5");
								// rang buoc 5: 4.6
								expr = new GRBLinExpr();
								expr.addTerm(1.0, y[i.getIndex()][k.getIndex()][j]);
								for (int t = 0; t < T; t++) {
									expr.addTerm(1.0, x[i.getIndex()][j][t]);
									expr.addTerm(1.0, z[i.getIndex()][j][t]);
									expr.addTerm(1.0, x[k.getIndex()][j][t]);
									expr.addTerm(1.0, z[k.getIndex()][j][t]);
								}
								model.addConstr(expr, GRB.LESS_EQUAL, 2, "contr4.6");
							}
		// Rang buoc kha nang cua cau bo: 4.7
		for (int t = 0; t < T; t++) {
			expr = new GRBLinExpr();
			for (ArrayList<Container> lst : vessel.getColumns())
				for (Container i : lst)
					for (int j = 0; j < J; j++) {
						expr.addTerm(1.0, x[i.getIndex()][j][t]);
						expr.addTerm(1.0, z[i.getIndex()][j][t]);
					}
			model.addConstr(expr, GRB.EQUAL, 1, "contr4.7");
		}
		// Rang buoc so container trong 1 cot: 4.8
		for (int j = 1; j < J; j++) {
			expr = new GRBLinExpr();
			for (ArrayList<Container> lst : vessel.getColumns())
				for (Container i : lst)
					for (int t = 0; t < T; t++) {
						expr.addTerm(1.0, x[i.getIndex()][j][t]);
						expr.addTerm(1.0, z[i.getIndex()][j][t]);
					}
			model.addConstr(expr, GRB.LESS_EQUAL, h, "contr4.8");
		}
		// Rang buoc so container ha bai het: 4.9
		expr = new GRBLinExpr();
		for (ArrayList<Container> lst : vessel.getColumns())
			for (Container i : lst)
				for (int j = 0; j < J; j++)
					for (int t = 0; t < T; t++)
						expr.addTerm(1.0, x[i.getIndex()][j][t]);
		model.addConstr(expr, GRB.EQUAL, nC, "contr4.9");
		// Rang buoc dich chuyen giua 2 vi tri: 4.10
		expr = new GRBLinExpr();
		for (ArrayList<Container> lst : vessel.getColumns())
			for (Container i : lst) {
				expr = new GRBLinExpr();
				for (int j = 0; j < J; j++)
					for (int t = 0; t < T; t++)
						expr.addTerm(1.0, x[i.getIndex()][j][t]);
				model.addConstr(expr, GRB.EQUAL, 1.0, "contr4.10");
			}
		// Rang buoc moi: 4.11
		for (ArrayList<Container> lst : vessel.getColumns())
			for (Container i : lst) {
				expr = new GRBLinExpr();
				for (int t = 0; t < T; t++) {
					expr.addTerm(1.0, x[i.getIndex()][0][t]);
					for (int j = 1; j < J; j++)
						expr.addTerm(-1.0, z[i.getIndex()][j][t]);
				}
				model.addConstr(expr, GRB.EQUAL, 0, "contr4.11");
			}
		// Rang buoc moi: 4.12
		for (ArrayList<Container> lst : vessel.getColumns())
			for (Container i : lst) {
				expr = new GRBLinExpr();
				for (int t = 0; t < T; t++) {
					expr.addTerm(t, x[i.getIndex()][0][t]);
					for (int j = 1; j < J; j++)
						expr.addTerm(-t, z[i.getIndex()][j][t]);
				}
				model.addConstr(expr, GRB.LESS_EQUAL, 0, "contr4.12");
			}

		// objective function 1
//		GRBLinExpr func = new GRBLinExpr();
//		for (ArrayList<Container> lst : vessel.getColumns())
//			for (Container i : lst)
//				for (int j = 1; j < J; j++)
//					for (int t = 0; t < T; t++)
//						func.addTerm(1.0, x[i.getIndex()][j][t]);
//		for (ArrayList<Container> lst : vessel.getColumns())
//			for (Container i : lst)
//				for (int t = 0; t < T; t++)
//					func.addTerm(2.0, x[i.getIndex()][0][t]);
//		model.setObjective(func, GRB.MINIMIZE);

		// objective function 2
		GRBLinExpr func = new GRBLinExpr();
		for (ArrayList<Container> lst : vessel.getColumns())
			for (Container i : lst)
				for (int j = 1; j < J; j++)
					for (int t = 0; t < T; t++) {
						func.addTerm(t, x[i.getIndex()][j][t]);
						func.addTerm(t, z[i.getIndex()][j][t]);
					}
		model.setObjective(func, GRB.MINIMIZE);

		// write model to files
		// model.write("output/Model.lp");
		// model.write("output/Model.mps");

		// solve problem
		model.optimize();
		// model.write("output/Model.sol");

		// get result
		if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
			objVal = model.get(GRB.DoubleAttr.ObjVal);
			lowerBound = model.get(GRB.DoubleAttr.ObjBound);
			runtime = model.get(GRB.DoubleAttr.Runtime);
			yard = new Yard(J);
			for (int i = 1; i <= this.nC; i++)
				for (int j = 0; j < J; j++)
					for (int t = 0; t < T; t++) {
						if (x[i][j][t].get(GRB.DoubleAttr.X) > 0.5) {
							for (ArrayList<Container> lst : vessel.getColumns())
								for (Container c : lst)
									if (c.getIndex() == i) {
										c.setT1(t);
										yard.addContainer(c, j);
									}

						}
						if (j != 0 && z[i][j][t].get(GRB.DoubleAttr.X) > 0.5) {
							for (ArrayList<Container> lst : vessel.getColumns())
								for (Container c : lst)
									if (c.getIndex() == i) {
										c.setT2(t);
										yard.addContainer(c, j);
									}
						}
					}
		}

	}

	private void printVariables(String fileName) throws Exception {
		PrintWriter out = new PrintWriter(fileName);
		for (int i = 1; i <= this.nC; i++)
			for (int j = 0; j < J; j++)
				for (int t = 0; t < T; t++)
					out.println("x[" + i + "][" + j + "][" + t + "] = " + x[i][j][t].get(GRB.DoubleAttr.X));
		for (int i = 1; i < this.nC; i++)
			for (int k = i + 1; k < this.nC + 1; k++)
				for (int t = 0; t < T; t++)
					out.println("y[" + i + "][" + k + "][" + t + "] = " + y[i][k][t].get(GRB.DoubleAttr.X));
		for (int i = 1; i <= this.nC; i++)
			for (int j = 0; j < J; j++)
				for (int t = 0; t < T; t++)
					out.println("z[" + i + "][" + j + "][" + t + "] = " + z[i][j][t].get(GRB.DoubleAttr.X));
		out.close();
	}

	private void saveData(String fileName) throws Exception {
		PrintWriter out = new PrintWriter(fileName);
		out.println("ObjVal:" + String.format("%.2f", objVal) + " ** LowerBound:" + String.format("%.2f", lowerBound)
				+ " ** Runtime(s):" + String.format("%.2f", runtime));
		out.println("---------------");
		for (int j = 0; j < yard.getColumns().length; j++) {
			ArrayList<Container> lst = yard.getColumns()[j];
			out.print("Col " + j + ": ");
			for (int i = lst.size() - 1; i >= 0; i--) {
				Container c = lst.get(i);
				if (j == 0) // Container duoc boc vao bai tam
					out.print(c.getIndex() + " (" + (c.getT1() + 1) + ")" + "\t");
				else {
					if (c.getT2() == -1) // Container duoc boc thang vao bai chinh
						out.print(c.getIndex() + " (" + (c.getT1() + 1) + ")" + "\t");
					else // Container duoc boc tu bai tam qua bai chinh
						out.print(c.getIndex() + " (" + (c.getT2() + 1) + ")" + "\t");
				}
			}
			out.println();
		}
		out.close();
	}
}