package com.xproger.arcanum;

import java.math.BigInteger;

public class ECM {

	private BigInteger n;

	private static final int TYP_EC = 300000000;

	public final BigInteger PD[] = new BigInteger[4000]; // and prime factors
	private final int Exp[] = new int[4000];
	private final int Typ[] = new int[4000];

	private static final BigInteger BigInt1 = BigInteger.valueOf(1L);
	private static final BigInteger BigInt2 = BigInteger.valueOf(2L);
	private static final BigInteger BigInt3 = BigInteger.valueOf(3L);
	private static final int NLen = 1200;
	private int NumberLength; /* Length of multiple precision nbrs */
	private int NbrFactors;
	private int EC; /* Elliptic Curve number */
	/* Used inside GCD calculations in multiple precision numbers */
	private final int CalcAuxGcdU[] = new int[NLen];
	private final int CalcAuxGcdV[] = new int[NLen];
	private final int CalcAuxGcdT[] = new int[NLen];
	private final long CalcAuxModInvA[] = new long[NLen];
	private final long CalcAuxModInvB[] = new long[NLen];
	private final long CalcAuxModInvBB[] = new long[NLen];
	private final long CalcAuxModInvMu[] = new long[NLen];
	private final long CalcAuxModInvGamma[] = new long[NLen];
	private int TestNbr[] = new int[NLen];
	private final int GcdAccumulated[] = new int[NLen];
	private int[] fieldAA, fieldTX, fieldTZ, fieldUX, fieldUZ;
	private int[] fieldAux1, fieldAux2, fieldAux3, fieldAux4;
	private static final long DosALa32 = (long) 1 << 32;
	private static final long DosALa31 = (long) 1 << 31;
	private static final double dDosALa31 = (double) DosALa31;
	private static final double dDosALa62 = dDosALa31 * dDosALa31;
	private static final long Mi = 1000000000;
	private final int BigNbr1[] = new int[NLen];
	private final int SmallPrime[] = new int[670]; /* Primes < 5000 */
	private final int MontgomeryMultR1[] = new int[NLen];
	private final int MontgomeryMultR2[] = new int[NLen];
	private final int MontgomeryMultAfterInv[] = new int[NLen];
	private long MontgomeryMultN;
	private boolean batchFinished = true;
	private boolean batchPrime = false;
	private int indexM, maxIndexM;

	public ECM(BigInteger n) {
		this.n = n;
	}

	private void InsertNewFactor(final BigInteger InputFactor) {
		int g, exp;

		/* Insert input factor */
		for (g = NbrFactors - 1; g >= 0; g--) {
			PD[NbrFactors] = PD[g].gcd(InputFactor);
			if (PD[NbrFactors].equals(BigInt1) || PD[NbrFactors].equals(PD[g])) {
				continue;
			}
			for (exp = 0; PD[g].remainder(PD[NbrFactors]).signum() == 0; exp++) {
				PD[g] = PD[g].divide(PD[NbrFactors]);
			}
			Exp[NbrFactors] = Exp[g] * exp;
			if (Typ[g] < 100000000) {
				Typ[g] = -EC;
				Typ[NbrFactors] = -TYP_EC - EC;
			}
			NbrFactors++;
		}
		SortFactorsInputNbr();
	}

	private void SortFactorsInputNbr() {
		int g, i, j;
		BigInteger Nbr1;

		for (g = 0; g < NbrFactors - 1; g++) {
			for (j = g + 1; j < NbrFactors; j++) {
				if (PD[g].compareTo(PD[j]) > 0) {
					Nbr1 = PD[g];
					PD[g] = PD[j];
					PD[j] = Nbr1;
					i = Exp[g];
					Exp[g] = Exp[j];
					Exp[j] = i;
					i = Typ[g];
					Typ[g] = Typ[j];
					Typ[j] = i;
				}
			}
		}
	}

	public void factorize() {
		BigInteger NN;
		long testComp;
		int i, j;

		if (n.abs().compareTo(BigInt1) <= 0) { // Factor number -1, 0 or 1.
			return;
		} else if (n.signum() < 0) {// Factor negative number.
			n = n.negate(); // Convert to positive.
		}
		BigNbr1[0] = 1;
		for (i = 1; i < NLen; i++) {
			BigNbr1[i] = 0;
		}
		try {
			if (NbrFactors == 0) {
				testComp = GetSmallFactors(n, PD, Exp, Typ, 0);
				if (testComp != 1) { // There are factors greater than 131071.
					PD[NbrFactors] = BigIntToBigNbr(TestNbr, NumberLength);
					Exp[NbrFactors] = 1;
					Typ[NbrFactors] = -1; /* Unknown */
					NbrFactors++;

				} else // No more factors
				{
					if (batchPrime && NbrFactors == 1 && Typ[0] == 0) {
						NbrFactors = 0; // Indicate number is prime.
						return;
					}
				}
			}
			factor_loop: for (;;) {
				for (i = 0; i < NbrFactors; i++) {
					if (Typ[i] < 0) { /* Unknown */
						if (PD[i].bitLength() <= 33) {
							j = 0;
						} else {
							j = n.compareTo(PD[i]) == 0 ? 1 : 0;
							if (!batchFinished && batchPrime) {
								if (NbrFactors < 2) { // If no factors found in trial factoring...
									NbrFactors = j;
								}
								return;
							}
						}
						if (j == 0) {
							if (Typ[i] < -TYP_EC) {
								Typ[i] = -Typ[i]; /* Prime */
							} else {
								Typ[i] = 0; /* Prime */
							}
						} else {
							if (Typ[i] < -TYP_EC) {
								Typ[i] = -TYP_EC - Typ[i]; /* Composite */
							} else {
								Typ[i] = -Typ[i]; /* Composite */
							}
						}
						continue factor_loop;
					}
				}
				for (i = 0; i < NbrFactors; i++) {
					EC = Typ[i];
					if (EC > 0 && EC < TYP_EC) { /* Composite */
						EC %= 50000000;
						NN = fnECM(PD[i], i);
						Typ[i] = EC;
						InsertNewFactor(NN);
						continue factor_loop;
					}
				}
				break;
			}
		} catch (ArithmeticException e) {
			System.gc();
			return;
		}
		System.gc();
	}

	private long GetSmallFactors(final BigInteger NumberToFactor, BigInteger PD[], int Exp[], int Typ[], final int Type) {

		long Div, TestComp;
		int i;
		boolean checkExpParity = false;

		NumberLength = BigNbrToBigInt(NumberToFactor, TestNbr);
		NbrFactors = 0;
		for (i = 0; i < 400; i++) {
			Exp[i] = Typ[i] = 0;
		}
		while ((TestNbr[0] & 1) == 0) { /* N even */
			if (Exp[NbrFactors] == 0) {
				PD[NbrFactors] = BigInt2;
			}
			Exp[NbrFactors]++;
			DivBigNbrByLong(TestNbr, 2, TestNbr, NumberLength);
		}
		if (Exp[NbrFactors] != 0) {
			NbrFactors++;
		}
		while (RemDivBigNbrByLong(TestNbr, 3, NumberLength) == 0) {
			if (Type == 1) {
				checkExpParity ^= true;
			}
			if (Exp[NbrFactors] == 0) {
				PD[NbrFactors] = BigInt3;
			}
			Exp[NbrFactors]++;
			DivBigNbrByLong(TestNbr, 3, TestNbr, NumberLength);
		}
		if (checkExpParity) {
			return -1; /* Discard it */
		}
		if (Exp[NbrFactors] != 0) {
			NbrFactors++;
		}
		Div = 5;
		TestComp = (long) TestNbr[0] + ((long) TestNbr[1] << 31);
		if (TestComp < 0) {
			TestComp = 10000 * DosALa31;
		} else {
			for (i = 2; i < NumberLength; i++) {
				if (TestNbr[i] != 0) {
					TestComp = 10000 * DosALa31;
					break;
				}
			}
		}
		while (Div < 131072) {
			if (Div % 3 != 0) {
				while (RemDivBigNbrByLong(TestNbr, Div, NumberLength) == 0) {
					if (Type == 1 && Div % 4 == 3) {
						checkExpParity ^= true;
					}
					if (Exp[NbrFactors] == 0) {
						PD[NbrFactors] = BigInteger.valueOf(Div);
					}
					Exp[NbrFactors]++;
					DivBigNbrByLong(TestNbr, Div, TestNbr, NumberLength);
					TestComp = (long) TestNbr[0] + ((long) TestNbr[1] << 31);
					if (TestComp < 0) {
						TestComp = 10000 * DosALa31;
					} else {
						for (i = 2; i < NumberLength; i++) {
							if (TestNbr[i] != 0) {
								TestComp = 10000 * DosALa31;
								break;
							}
						}
					} /* end while */
				}
				if (checkExpParity) {
					return -1; /* Discard it */
				}
				if (Exp[NbrFactors] != 0) {
					NbrFactors++;
				}
			}
			Div += 2;
			if (TestComp < Div * Div && TestComp != 1) {
				if (Type == 1 && TestComp % 4 == 3) {
					return -1; /* Discard it */
				}
				if (Exp[NbrFactors] != 0) {
					NbrFactors++;
				}
				PD[NbrFactors] = BigInteger.valueOf(TestComp);
				Exp[NbrFactors] = 1;
				TestComp = 1;
				NbrFactors++;
				break;
			}
		} /* end while */
		return TestComp;
	}

	private BigInteger fnECM(BigInteger N, int FactorIndex) {
		int I, J, Pass, Qaux;
		long L1, L2, LS, P, Q, IP;
		int[] A0 = new int[NLen];
		int[] A02 = new int[NLen];
		int[] A03 = new int[NLen];
		int[] AA = new int[NLen];
		int[] DX = new int[NLen];
		int[] DZ = new int[NLen];
		int[] GD = new int[NLen];
		int[] M = new int[NLen];
		int[] TX = new int[NLen];
		fieldTX = TX;
		int[] TZ = new int[NLen];
		fieldTZ = TZ;
		int[] UX = new int[NLen];
		fieldUX = UX;
		int[] UZ = new int[NLen];
		fieldUZ = UZ;
		int[] W1 = new int[NLen];
		int[] W2 = new int[NLen];
		int[] W3 = new int[NLen];
		int[] W4 = new int[NLen];
		int[] WX = new int[NLen];
		int[] WZ = new int[NLen];
		int[] X = new int[NLen];
		int[] Z = new int[NLen];
		int[] Aux1 = new int[NLen];
		fieldAux1 = Aux1;
		int[] Aux2 = new int[NLen];
		fieldAux2 = Aux2;
		int[] Aux3 = new int[NLen];
		fieldAux3 = Aux3;
		int[] Aux4 = new int[NLen];
		fieldAux4 = Aux4;
		int[] Xaux = new int[NLen];
		int[] Zaux = new int[NLen];
		int[][] root = new int[480][NLen];
		byte[] sieve = new byte[23100];
		byte[] sieve2310 = new byte[2310];
		int[] sieveidx = new int[480];
		int i, j, u;

		fieldAA = AA;
		NumberLength = BigNbrToBigInt(N, TestNbr);
		GetMontgomeryParms();
		for (I = 0; I < NumberLength; I++) {
			M[I] = DX[I] = DZ[I] = W3[I] = W4[I] = GD[I] = 0;
		}
		EC--;
		SmallPrime[0] = 2;
		P = 3;
		indexM = 1;
		for (indexM = 1; indexM < SmallPrime.length; indexM++) {
			SmallPrime[indexM] = (int) P; /* Store prime */
			calculate_new_prime1: for (;;) {
				P += 2;
				for (Q = 3; Q * Q <= P; Q += 2) { /* Check if P is prime */
					if (P % Q == 0) {
						continue calculate_new_prime1; /* Composite */
					}
				}
				break; /* Prime found */
			}
		}
		do {
			new_curve: for (;;) {
				EC++;
				Typ[FactorIndex] = EC;
				L1 = 2000;
				L2 = 200000;
				LS = 45;
				if (EC > 25) {
					if (EC < 326) {
						L1 = 50000;
						L2 = 5000000;
						LS = 224;
					} else {
						if (EC < 2000) {
							L1 = 1000000;
							L2 = 100000000;
							LS = 1001;
						} else {
							L1 = 11000000;
							L2 = 1100000000;
							LS = 3316;
						}
					}
				}
				/* end for */
				
				LongToBigNbr(2 * (EC + 1), Aux1, NumberLength);
				LongToBigNbr(3 * (EC + 1) * (EC + 1) - 1, Aux2, NumberLength);
				ModInvBigNbr(Aux2, Aux2, TestNbr, NumberLength);
				MultBigNbrModN(Aux1, Aux2, Aux3, TestNbr, NumberLength);
				MultBigNbrModN(Aux3, MontgomeryMultR1, A0, TestNbr, NumberLength);
				MontgomeryMult(A0, A0, A02);
				MontgomeryMult(A02, A0, A03);
				SubtractBigNbrModN(A03, A0, Aux1, TestNbr, NumberLength);
				MultBigNbrByLongModN(A02, 9, Aux2, TestNbr, NumberLength);
				SubtractBigNbrModN(Aux2, MontgomeryMultR1, Aux2, TestNbr, NumberLength);
				MontgomeryMult(Aux1, Aux2, Aux3);
				if (BigNbrIsZero(Aux3)) {
					continue;
				}
				MultBigNbrByLongModN(A0, 4, Z, TestNbr, NumberLength);
				MultBigNbrByLongModN(A02, 6, Aux1, TestNbr, NumberLength);
				SubtractBigNbrModN(MontgomeryMultR1, Aux1, Aux1, TestNbr, NumberLength);
				MontgomeryMult(A02, A02, Aux2);
				MultBigNbrByLongModN(Aux2, 3, Aux2, TestNbr, NumberLength);
				SubtractBigNbrModN(Aux1, Aux2, Aux1, TestNbr, NumberLength);
				MultBigNbrByLongModN(A03, 4, Aux2, TestNbr, NumberLength);
				ModInvBigNbr(Aux2, Aux2, TestNbr, NumberLength);
				MontgomeryMult(Aux2, MontgomeryMultAfterInv, Aux3);
				MontgomeryMult(Aux1, Aux3, A0);
				AddBigNbrModN(A0, MontgomeryMultR2, Aux1, TestNbr, NumberLength);
				LongToBigNbr(4, Aux2, NumberLength);
				ModInvBigNbr(Aux2, Aux3, TestNbr, NumberLength);
				MultBigNbrModN(Aux3, MontgomeryMultR1, Aux2, TestNbr, NumberLength);
				MontgomeryMult(Aux1, Aux2, AA);
				MultBigNbrByLongModN(A02, 3, Aux1, TestNbr, NumberLength);
				AddBigNbrModN(Aux1, MontgomeryMultR1, X, TestNbr, NumberLength);
				/**************/
				/* First step */
				/**************/
				System.arraycopy(X, 0, Xaux, 0, NumberLength);
				System.arraycopy(Z, 0, Zaux, 0, NumberLength);
				System.arraycopy(MontgomeryMultR1, 0, GcdAccumulated, 0, NumberLength);
				for (Pass = 0; Pass < 2; Pass++) {
					/* For powers of 2 */
					for (I = 1; I <= L1; I <<= 1) {
						duplicate(X, Z, X, Z);
					}
					for (I = 3; I <= L1; I *= 3) {
						duplicate(W1, W2, X, Z);
						add3(X, Z, X, Z, W1, W2, X, Z);
					}

					if (Pass == 0) {
						MontgomeryMult(GcdAccumulated, Z, Aux1);
						System.arraycopy(Aux1, 0, GcdAccumulated, 0, NumberLength);
					} else {
						GcdBigNbr(Z, TestNbr, GD, NumberLength);
						if (!BigNbrAreEqual(GD, BigNbr1)) {
							break new_curve;
						}
					}

					/* for powers of odd primes */

					indexM = 1;
					do {
						P = SmallPrime[indexM];
						for (IP = P; IP <= L1; IP *= P) {
							prac((int) P, X, Z, W1, W2, W3, W4);
						}
						indexM++;
						if (Pass == 0) {
							MontgomeryMult(GcdAccumulated, Z, Aux1);
							System.arraycopy(Aux1, 0, GcdAccumulated, 0, NumberLength);
						} else {
							GcdBigNbr(Z, TestNbr, GD, NumberLength);
							if (!BigNbrAreEqual(GD, BigNbr1)) {
								break new_curve;
							}
						}
					} while (SmallPrime[indexM - 1] <= LS);
					P += 2;

					/*
					 * Initialize sieve2310[n]: 1 if gcd(P+2n,2310) > 1, 0
					 * otherwise
					 */
					u = (int) P;
					for (i = 0; i < 2310; i++) {
						sieve2310[i] = (u % 3 == 0 || u % 5 == 0 || u % 7 == 0 || u % 11 == 0 ? (byte) 1 : (byte) 0);
						u += 2;
					}
					do {
						/* Generate sieve */
						GenerateSieve((int) P, sieve, sieve2310, SmallPrime);

						/* Walk through sieve */

						for (i = 0; i < 23100; i++) {
							if (sieve[i] != 0)
								continue; /* Do not process composites */
							if (P + 2 * i > L1)
								break;

							prac((int) (P + 2 * i), X, Z, W1, W2, W3, W4);
							if (Pass == 0) {
								MontgomeryMult(GcdAccumulated, Z, Aux1);
								System.arraycopy(Aux1, 0, GcdAccumulated, 0, NumberLength);
							} else {
								GcdBigNbr(Z, TestNbr, GD, NumberLength);
								if (!BigNbrAreEqual(GD, BigNbr1)) {
									break new_curve;
								}
							}
						}
						P += 46200;
					} while (P < L1);
					if (Pass == 0) {
						if (BigNbrIsZero(GcdAccumulated)) { // If GcdAccumulated is
							System.arraycopy(Xaux, 0, X, 0, NumberLength);
							System.arraycopy(Zaux, 0, Z, 0, NumberLength);
							continue; // multiple of TestNbr, continue.
						}
						GcdBigNbr(GcdAccumulated, TestNbr, GD, NumberLength);
						if (!BigNbrAreEqual(GD, BigNbr1)) {
							break new_curve;
						}
						break;
					}
				} /* end for Pass */

				/******************************************************/
				/* Second step (using improved standard continuation) */
				/******************************************************/
				j = 0;
				for (u = 1; u < 2310; u += 2) {
					if (u % 3 == 0 || u % 5 == 0 || u % 7 == 0 || u % 11 == 0) {
						sieve2310[u / 2] = (byte) 1;
					} else {
						sieve2310[(sieveidx[j++] = u / 2)] = (byte) 0;
					}
				}
				System.arraycopy(sieve2310, 0, sieve2310, 1155, 1155);
				System.arraycopy(X, 0, Xaux, 0, NumberLength); // (X:Z) -> Q
																// (output
				System.arraycopy(Z, 0, Zaux, 0, NumberLength); // from step 1)
				for (Pass = 0; Pass < 2; Pass++) {
					System.arraycopy(MontgomeryMultR1, 0, GcdAccumulated, 0, NumberLength);
					System.arraycopy(X, 0, UX, 0, NumberLength);
					System.arraycopy(Z, 0, UZ, 0, NumberLength); // (UX:UZ) -> Q
					ModInvBigNbr(Z, Aux2, TestNbr, NumberLength);
					MontgomeryMult(Aux2, MontgomeryMultAfterInv, Aux1);
					MontgomeryMult(Aux1, X, root[0]); // root[0] <- X/Z (Q)
					J = 0;
					AddBigNbrModN(X, Z, Aux1, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux1, W1);
					SubtractBigNbrModN(X, Z, Aux1, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux1, W2);
					MontgomeryMult(W1, W2, TX);
					SubtractBigNbrModN(W1, W2, Aux1, TestNbr, NumberLength);
					MontgomeryMult(Aux1, AA, Aux2);
					AddBigNbrModN(Aux2, W2, Aux3, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux3, TZ); // (TX:TZ) -> 2Q
					SubtractBigNbrModN(X, Z, Aux1, TestNbr, NumberLength);
					AddBigNbrModN(TX, TZ, Aux2, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux2, W1);
					AddBigNbrModN(X, Z, Aux1, TestNbr, NumberLength);
					SubtractBigNbrModN(TX, TZ, Aux2, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux2, W2);
					AddBigNbrModN(W1, W2, Aux1, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux1, Aux2);
					MontgomeryMult(Aux2, UZ, X);
					SubtractBigNbrModN(W1, W2, Aux1, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux1, Aux2);
					MontgomeryMult(Aux2, UX, Z); // (X:Z) -> 3Q
					for (I = 5; I < 2310; I += 2) {
						System.arraycopy(X, 0, WX, 0, NumberLength);
						System.arraycopy(Z, 0, WZ, 0, NumberLength);
						SubtractBigNbrModN(X, Z, Aux1, TestNbr, NumberLength);
						AddBigNbrModN(TX, TZ, Aux2, TestNbr, NumberLength);
						MontgomeryMult(Aux1, Aux2, W1);
						AddBigNbrModN(X, Z, Aux1, TestNbr, NumberLength);
						SubtractBigNbrModN(TX, TZ, Aux2, TestNbr, NumberLength);
						MontgomeryMult(Aux1, Aux2, W2);
						AddBigNbrModN(W1, W2, Aux1, TestNbr, NumberLength);
						MontgomeryMult(Aux1, Aux1, Aux2);
						MontgomeryMult(Aux2, UZ, X);
						SubtractBigNbrModN(W1, W2, Aux1, TestNbr, NumberLength);
						MontgomeryMult(Aux1, Aux1, Aux2);
						MontgomeryMult(Aux2, UX, Z); // (X:Z) -> 5Q, 7Q, ...
						if (Pass == 0) {
							MontgomeryMult(GcdAccumulated, Aux1, Aux2);
							System.arraycopy(Aux2, 0, GcdAccumulated, 0, NumberLength);
						} else {
							GcdBigNbr(Aux1, TestNbr, GD, NumberLength);
							if (!BigNbrAreEqual(GD, BigNbr1)) {
								break new_curve;
							}
						}
						if (I == 1155) {
							System.arraycopy(X, 0, DX, 0, NumberLength);
							System.arraycopy(Z, 0, DZ, 0, NumberLength); // (DX:DZ)
																			// ->
																			// 1155Q
						}
						if (I % 3 != 0 && I % 5 != 0 && I % 7 != 0 && I % 11 != 0) {
							J++;
							ModInvBigNbr(Z, Aux2, TestNbr, NumberLength);
							MontgomeryMult(Aux2, MontgomeryMultAfterInv, Aux1);
							MontgomeryMult(Aux1, X, root[J]); // root[J] <- X/Z
						}
						System.arraycopy(WX, 0, UX, 0, NumberLength); // (UX:UZ)
																		// <-
						System.arraycopy(WZ, 0, UZ, 0, NumberLength); // Previous
																		// (X:Z)
					} /* end for I */
					AddBigNbrModN(DX, DZ, Aux1, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux1, W1);
					SubtractBigNbrModN(DX, DZ, Aux1, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux1, W2);
					MontgomeryMult(W1, W2, X);
					SubtractBigNbrModN(W1, W2, Aux1, TestNbr, NumberLength);
					MontgomeryMult(Aux1, AA, Aux2);
					AddBigNbrModN(Aux2, W2, Aux3, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux3, Z);
					System.arraycopy(X, 0, UX, 0, NumberLength);
					System.arraycopy(Z, 0, UZ, 0, NumberLength); // (UX:UZ) ->
																	// 2310Q
					AddBigNbrModN(X, Z, Aux1, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux1, W1);
					SubtractBigNbrModN(X, Z, Aux1, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux1, W2);
					MontgomeryMult(W1, W2, TX);
					SubtractBigNbrModN(W1, W2, Aux1, TestNbr, NumberLength);
					MontgomeryMult(Aux1, AA, Aux2);
					AddBigNbrModN(Aux2, W2, Aux3, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux3, TZ); // (TX:TZ) -> 2*2310Q
					SubtractBigNbrModN(X, Z, Aux1, TestNbr, NumberLength);
					AddBigNbrModN(TX, TZ, Aux2, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux2, W1);
					AddBigNbrModN(X, Z, Aux1, TestNbr, NumberLength);
					SubtractBigNbrModN(TX, TZ, Aux2, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux2, W2);
					AddBigNbrModN(W1, W2, Aux1, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux1, Aux2);
					MontgomeryMult(Aux2, UZ, X);
					SubtractBigNbrModN(W1, W2, Aux1, TestNbr, NumberLength);
					MontgomeryMult(Aux1, Aux1, Aux2);
					MontgomeryMult(Aux2, UX, Z); // (X:Z) -> 3*2310Q
					Qaux = (int) (L1 / 4620);
					maxIndexM = (int) (L2 / 4620);
					for (indexM = 0; indexM <= maxIndexM; indexM++) {
						if (indexM >= Qaux) { // If inside step 2 range...
							if (indexM == 0) {
								ModInvBigNbr(UZ, Aux2, TestNbr, NumberLength);
								MontgomeryMult(Aux2, MontgomeryMultAfterInv, Aux3);
								MontgomeryMult(UX, Aux3, Aux1); // Aux1 <- X/Z
																// (2310Q)
							} else {
								ModInvBigNbr(Z, Aux2, TestNbr, NumberLength);
								MontgomeryMult(Aux2, MontgomeryMultAfterInv, Aux3);
								MontgomeryMult(X, Aux3, Aux1); // Aux1 <- X/Z
																// (3,5,*
							} // 2310Q)

							/* Generate sieve */
							if (indexM % 10 == 0 || indexM == Qaux) {
								GenerateSieve(indexM / 10 * 46200 + 1, sieve, sieve2310, SmallPrime);
							}
							/* Walk through sieve */
							J = 1155 + (indexM % 10) * 2310;
							for (i = 0; i < 480; i++) {
								j = sieveidx[i]; // 0 < J < 1155
								if (sieve[J + j] != 0 && sieve[J - 1 - j] != 0) {
									continue; // Do not process if both are
												// composite numbers.
								}
								SubtractBigNbrModN(Aux1, root[i], M, TestNbr, NumberLength);
								MontgomeryMult(GcdAccumulated, M, Aux2);
								System.arraycopy(Aux2, 0, GcdAccumulated, 0, NumberLength);
							}
							if (Pass != 0) {
								GcdBigNbr(GcdAccumulated, TestNbr, GD, NumberLength);
								if (!BigNbrAreEqual(GD, BigNbr1)) {
									break new_curve;
								}
							}
						}
						if (indexM != 0) { // Update (X:Z)
							System.arraycopy(X, 0, WX, 0, NumberLength);
							System.arraycopy(Z, 0, WZ, 0, NumberLength);
							SubtractBigNbrModN(X, Z, Aux1, TestNbr, NumberLength);
							AddBigNbrModN(TX, TZ, Aux2, TestNbr, NumberLength);
							MontgomeryMult(Aux1, Aux2, W1);
							AddBigNbrModN(X, Z, Aux1, TestNbr, NumberLength);
							SubtractBigNbrModN(TX, TZ, Aux2, TestNbr, NumberLength);
							MontgomeryMult(Aux1, Aux2, W2);
							AddBigNbrModN(W1, W2, Aux1, TestNbr, NumberLength);
							MontgomeryMult(Aux1, Aux1, Aux2);
							MontgomeryMult(Aux2, UZ, X);
							SubtractBigNbrModN(W1, W2, Aux1, TestNbr, NumberLength);
							MontgomeryMult(Aux1, Aux1, Aux2);
							MontgomeryMult(Aux2, UX, Z);
							System.arraycopy(WX, 0, UX, 0, NumberLength);
							System.arraycopy(WZ, 0, UZ, 0, NumberLength);
						}
					} // end for Q
					if (Pass == 0) {
						if (BigNbrIsZero(GcdAccumulated)) { // If GcdAccumulated
															// is
							System.arraycopy(Xaux, 0, X, 0, NumberLength);
							System.arraycopy(Zaux, 0, Z, 0, NumberLength);
							continue; // multiple of TestNbr, continue.
						}
						GcdBigNbr(GcdAccumulated, TestNbr, GD, NumberLength);
						if (BigNbrAreEqual(GD, TestNbr)) {
							break;
						}
						if (!BigNbrAreEqual(GD, BigNbr1)) {
							break new_curve;
						}
						break;
					}
				} /* end for Pass */
			} /* End curve calculation */
		} while (BigNbrAreEqual(GD, TestNbr));
		return BigIntToBigNbr(GD, NumberLength);
	}

	/**********************/
	/* Auxiliary routines */
	/**********************/

	private static void GenerateSieve(int initial, byte[] sieve, byte[] sieve2310, int[] SmallPrime) {
		int i, j, Q;
		for (i = 0; i < 23100; i += 2310) {
			System.arraycopy(sieve2310, 0, sieve, i, 2310);
		}
		j = 5;
		Q = 13; /* Point to prime 13 */
		do {
			if (initial > Q * Q) {
				for (i = (int) (((long) initial * ((Q - 1) / 2)) % Q); i < 23100; i += Q) {
					sieve[i] = 1; /* Composite */
				}
			} else {
				i = Q * Q - initial;
				if (i < 46200) {
					for (i = i / 2; i < 23100; i += Q) {
						sieve[i] = 1; /* Composite */
					}
				} else {
					break;
				}
			}
			Q = SmallPrime[++j];
		} while (Q < 5000);
	}

	private static int BigNbrToBigInt(BigInteger N, int TestNbr[]) {
		byte[] Result;
		long[] Temp;
		int i, j, mask;
		long p;
		int NumberLength;

		Result = N.toByteArray();
		NumberLength = (Result.length * 8 + 30) / 31;
		Temp = new long[NumberLength + 1];
		j = 0;
		mask = 1;
		p = 0;
		for (i = Result.length - 1; i >= 0; i--) {
			p += mask * (long) (Result[i] >= 0 ? Result[i] : Result[i] + 256);
			mask <<= 8;
			if (mask == 0) { // Overflow
				Temp[j++] = p;
				mask = 1;
				p = 0;
			}
		}
		Temp[j] = p;
		Convert32To31Bits(Temp, TestNbr, NumberLength);
		if (TestNbr[NumberLength - 1] > Mi) {
			TestNbr[NumberLength] = 0;
			NumberLength++;
		}
		TestNbr[NumberLength] = 0;
		return NumberLength;
	}

	/*********************************************************/
	/* Start of code "borrowed" from Paul Zimmermann's ECM4C */
	/*********************************************************/
	private final static int ADD = 6; /* number of multiplications in an addition */
	private final static int DUP = 5; /* number of multiplications in a duplicate */

	/* returns the number of modular multiplications */
	private static int lucas_cost(int n, double v) {
		int c, d, e, r;

		d = n;
		r = (int) ((double) d / v + 0.5);
		if (r >= n)
			return (ADD * n);
		d = n - r;
		e = 2 * r - n;
		c = DUP + ADD; /* initial duplicate and final addition */
		while (d != e) {
			if (d < e) {
				r = d;
				d = e;
				e = r;
			}
			if (4 * d <= 5 * e && ((d + e) % 3) == 0) { /* condition 1 */
				r = (2 * d - e) / 3;
				e = (2 * e - d) / 3;
				d = r;
				c += 3 * ADD; /* 3 additions */
			} else if (4 * d <= 5 * e && (d - e) % 6 == 0) { /* condition 2 */
				d = (d - e) / 2;
				c += ADD + DUP; /* one addition, one duplicate */
			} else if (d <= (4 * e)) { /* condition 3 */
				d -= e;
				c += ADD; /* one addition */
			} else if ((d + e) % 2 == 0) { /* condition 4 */
				d = (d - e) / 2;
				c += ADD + DUP; /* one addition, one duplicate */
			} else if (d % 2 == 0) { /* condition 5 */
				d /= 2;
				c += ADD + DUP; /* one addition, one duplicate */
			} else if (d % 3 == 0) { /* condition 6 */
				d = d / 3 - e;
				c += 3 * ADD + DUP; /* three additions, one duplicate */
			} else if ((d + e) % 3 == 0) { /* condition 7 */
				d = (d - 2 * e) / 3;
				c += 3 * ADD + DUP; /* three additions, one duplicate */
			} else if ((d - e) % 3 == 0) { /* condition 8 */
				d = (d - e) / 3;
				c += 3 * ADD + DUP; /* three additions, one duplicate */
			} else if (e % 2 == 0) { /* condition 9 */
				e /= 2;
				c += ADD + DUP; /* one addition, one duplicate */
			}
		}
		return (c);
	}

	/* computes nP from P=(x:z) and puts the result in (x:z). Assumes n>2. */
	private void prac(int n, int[] x, int[] z, int[] xT, int[] zT, int[] xT2, int[] zT2) {
		int d, e, r, i;
		int[] t;
		int[] xA = x, zA = z;
		int[] xB = fieldAux1, zB = fieldAux2;
		int[] xC = fieldAux3, zC = fieldAux4;
		double v[] = { 1.61803398875, 1.72360679775, 1.618347119656, 1.617914406529, 1.612429949509, 1.632839806089, 1.620181980807, 1.580178728295,
				1.617214616534, 1.38196601125 };

		/* chooses the best value of v */
		r = lucas_cost(n, v[0]);
		i = 0;
		for (d = 1; d < 10; d++) {
			e = lucas_cost(n, v[d]);
			if (e < r) {
				r = e;
				i = d;
			}
		}
		d = n;
		r = (int) ((double) d / v[i] + 0.5);
		/* first iteration always begins by Condition 3, then a swap */
		d = n - r;
		e = 2 * r - n;
		System.arraycopy(xA, 0, xB, 0, NumberLength); // B = A
		System.arraycopy(zA, 0, zB, 0, NumberLength);
		System.arraycopy(xA, 0, xC, 0, NumberLength); // C = A
		System.arraycopy(zA, 0, zC, 0, NumberLength);
		duplicate(xA, zA, xA, zA); /* A=2*A */
		while (d != e) {
			if (d < e) {
				r = d;
				d = e;
				e = r;
				t = xA;
				xA = xB;
				xB = t;
				t = zA;
				zA = zB;
				zB = t;
			}
			/* do the first line of Table 4 whose condition qualifies */
			if (4 * d <= 5 * e && ((d + e) % 3) == 0) { /* condition 1 */
				r = (2 * d - e) / 3;
				e = (2 * e - d) / 3;
				d = r;
				add3(xT, zT, xA, zA, xB, zB, xC, zC); /* T = f(A,B,C) */
				add3(xT2, zT2, xT, zT, xA, zA, xB, zB); /* T2 = f(T,A,B) */
				add3(xB, zB, xB, zB, xT, zT, xA, zA); /* B = f(B,T,A) */
				t = xA;
				xA = xT2;
				xT2 = t;
				t = zA;
				zA = zT2;
				zT2 = t; /* swap A and T2 */
			} else if (4 * d <= 5 * e && (d - e) % 6 == 0) { /* condition 2 */
				d = (d - e) / 2;
				add3(xB, zB, xA, zA, xB, zB, xC, zC); /* B = f(A,B,C) */
				duplicate(xA, zA, xA, zA); /* A = 2*A */
			} else if (d <= (4 * e)) { /* condition 3 */
				d -= e;
				add3(xT, zT, xB, zB, xA, zA, xC, zC); /* T = f(B,A,C) */
				t = xB;
				xB = xT;
				xT = xC;
				xC = t;
				t = zB;
				zB = zT;
				zT = zC;
				zC = t; /* circular permutation (B,T,C) */
			} else if ((d + e) % 2 == 0) { /* condition 4 */
				d = (d - e) / 2;
				add3(xB, zB, xB, zB, xA, zA, xC, zC); /* B = f(B,A,C) */
				duplicate(xA, zA, xA, zA); /* A = 2*A */
			} else if (d % 2 == 0) { /* condition 5 */
				d /= 2;
				add3(xC, zC, xC, zC, xA, zA, xB, zB); /* C = f(C,A,B) */
				duplicate(xA, zA, xA, zA); /* A = 2*A */
			} else if (d % 3 == 0) { /* condition 6 */
				d = d / 3 - e;
				duplicate(xT, zT, xA, zA); /* T1 = 2*A */
				add3(xT2, zT2, xA, zA, xB, zB, xC, zC); /* T2 = f(A,B,C) */
				add3(xA, zA, xT, zT, xA, zA, xA, zA); /* A = f(T1,A,A) */
				add3(xT, zT, xT, zT, xT2, zT2, xC, zC); /* T1 = f(T1,T2,C) */
				t = xC;
				xC = xB;
				xB = xT;
				xT = t;
				t = zC;
				zC = zB;
				zB = zT;
				zT = t; /* circular permutation (C,B,T) */
			} else if ((d + e) % 3 == 0) { /* condition 7 */
				d = (d - 2 * e) / 3;
				add3(xT, zT, xA, zA, xB, zB, xC, zC); /* T1 = f(A,B,C) */
				add3(xB, zB, xT, zT, xA, zA, xB, zB); /* B = f(T1,A,B) */
				duplicate(xT, zT, xA, zA);
				add3(xA, zA, xA, zA, xT, zT, xA, zA); /* A = 3*A */
			} else if ((d - e) % 3 == 0) { /* condition 8 */
				d = (d - e) / 3;
				add3(xT, zT, xA, zA, xB, zB, xC, zC); /* T1 = f(A,B,C) */
				add3(xC, zC, xC, zC, xA, zA, xB, zB); /* C = f(A,C,B) */
				t = xB;
				xB = xT;
				xT = t;
				t = zB;
				zB = zT;
				zT = t; /* swap B and T */
				duplicate(xT, zT, xA, zA);
				add3(xA, zA, xA, zA, xT, zT, xA, zA); /* A = 3*A */
			} else if (e % 2 == 0) { /* condition 9 */
				e /= 2;
				add3(xC, zC, xC, zC, xB, zB, xA, zA); /* C = f(C,B,A) */
				duplicate(xB, zB, xB, zB); /* B = 2*B */
			}
		}
		add3(x, z, xA, zA, xB, zB, xC, zC);
	}

	/*
	 * adds Q=(x2:z2) and R=(x1:z1) and puts the result in (x3:z3), using 5/6
	 * mul, 6 add/sub and 6 mod. One assumes that Q-R=P or R-Q=P where P=(x:z).
	 * Uses the following global variables: - n : number to factor - x, z :
	 * coordinates of P - u, v, w : auxiliary variables Modifies: x3, z3, u, v,
	 * w. (x3,z3) may be identical to (x2,z2) and to (x,z)
	 */
	private void add3(int[] x3, int[] z3, int[] x2, int[] z2, int[] x1, int[] z1, int[] x, int[] z) {
		int[] t = fieldTX;
		int[] u = fieldTZ;
		int[] v = fieldUX;
		int[] w = fieldUZ;
		SubtractBigNbrModN(x2, z2, v, TestNbr, NumberLength); // v = x2-z2
		AddBigNbrModN(x1, z1, w, TestNbr, NumberLength); // w = x1+z1
		MontgomeryMult(v, w, u); // u = (x2-z2)*(x1+z1)
		AddBigNbrModN(x2, z2, w, TestNbr, NumberLength); // w = x2+z2
		SubtractBigNbrModN(x1, z1, t, TestNbr, NumberLength); // t = x1-z1
		MontgomeryMult(t, w, v); // v = (x2+z2)*(x1-z1)
		AddBigNbrModN(u, v, t, TestNbr, NumberLength); // t = 2*(x1*x2-z1*z2)
		MontgomeryMult(t, t, w); // w = 4*(x1*x2-z1*z2)^2
		SubtractBigNbrModN(u, v, t, TestNbr, NumberLength); // t =
															// 2*(x2*z1-x1*z2)
		MontgomeryMult(t, t, v); // v = 4*(x2*z1-x1*z2)^2
		if (BigNbrAreEqual(x, x3)) {
			System.arraycopy(x, 0, u, 0, NumberLength);
			System.arraycopy(w, 0, t, 0, NumberLength);
			MontgomeryMult(z, t, w);
			MontgomeryMult(v, u, z3);
			System.arraycopy(w, 0, x3, 0, NumberLength);
		} else {
			MontgomeryMult(w, z, x3); // x3 = 4*z*(x1*x2-z1*z2)^2
			MontgomeryMult(x, v, z3); // z3 = 4*x*(x2*z1-x1*z2)^2
		}
	}

	/*
	 * computes 2P=(x2:z2) from P=(x1:z1), with 5 mul, 4 add/sub, 5 mod. Uses
	 * the following global variables: - n : number to factor - b : (a+2)/4 mod
	 * n - u, v, w : auxiliary variables Modifies: x2, z2, u, v, w
	 */
	private void duplicate(int[] x2, int[] z2, int[] x1, int[] z1) {
		int[] u = fieldUZ;
		int[] v = fieldTX;
		int[] w = fieldTZ;
		AddBigNbrModN(x1, z1, w, TestNbr, NumberLength); // w = x1+z1
		MontgomeryMult(w, w, u); // u = (x1+z1)^2
		SubtractBigNbrModN(x1, z1, w, TestNbr, NumberLength); // w = x1-z1
		MontgomeryMult(w, w, v); // v = (x1-z1)^2
		MontgomeryMult(u, v, x2); // x2 = u*v = (x1^2 - z1^2)^2
		SubtractBigNbrModN(u, v, w, TestNbr, NumberLength); // w = u-v = 4*x1*z1
		MontgomeryMult(fieldAA, w, u);
		AddBigNbrModN(u, v, u, TestNbr, NumberLength); // u = (v+b*w)
		MontgomeryMult(w, u, z2); // z2 = (w*u)
	}

	/* End of code "borrowed" from Paul Zimmermann's ECM4C */

	private static BigInteger BigIntToBigNbr(int[] GD, int NumberLength) {
		byte[] Result;
		long[] Temp;
		int i, NL;
		long digit;

		Temp = new long[NumberLength];
		Convert31To32Bits(GD, Temp, NumberLength);
		NL = NumberLength * 4;
		Result = new byte[NL];
		for (i = 0; i < NumberLength; i++) {
			digit = Temp[i];
			Result[NL - 1 - 4 * i] = (byte) (digit & 0xFF);
			Result[NL - 2 - 4 * i] = (byte) (digit / 0x100 & 0xFF);
			Result[NL - 3 - 4 * i] = (byte) (digit / 0x10000 & 0xFF);
			Result[NL - 4 - 4 * i] = (byte) (digit / 0x1000000 & 0xFF);
		}
		return (new BigInteger(Result));
	}

	private static void LongToBigNbr(long Nbr, int Out[], int NumberLength) {
		int i;

		Out[0] = (int) (Nbr & 0x7FFFFFFF);
		Out[1] = (int) ((Nbr >> 31) & 0x7FFFFFFF);
		for (i = 2; i < NumberLength; i++) {
			Out[i] = (Nbr < 0 ? 0x7FFFFFFF : 0);
		}
	}

	private boolean BigNbrIsZero(int Nbr[]) {
		for (int i = 0; i < NumberLength; i++) {
			if (Nbr[i] != 0) {
				return false;
			}
		}
		return true;
	}

	private boolean BigNbrAreEqual(int Nbr1[], int Nbr2[]) {
		for (int i = 0; i < NumberLength; i++) {
			if (Nbr1[i] != Nbr2[i]) {
				return false;
			}
		}
		return true;
	}

	private static void ChSignBigNbr(int Nbr[], int NumberLength) {
		int carry = 0;
		for (int i = 0; i < NumberLength; i++) {
			carry = (carry >> 31) - Nbr[i];
			Nbr[i] = carry & 0x7FFFFFFF;
		}
	}

	private static void AddBigNbr(int Nbr1[], int Nbr2[], int Sum[], int NumberLength) {
		long carry = 0;
		for (int i = 0; i < NumberLength; i++) {
			carry = (carry >> 31) + (long) Nbr1[i] + (long) Nbr2[i];
			Sum[i] = (int) (carry & 0x7FFFFFFFL);
		}
	}

	private static void SubtractBigNbr(int Nbr1[], int Nbr2[], int Diff[], int NumberLength) {
		long carry = 0;
		for (int i = 0; i < NumberLength; i++) {
			carry = (carry >> 31) + (long) Nbr1[i] - (long) Nbr2[i];
			Diff[i] = (int) (carry & 0x7FFFFFFFL);
		}
	}

	private void AddBigNbr32(long Nbr1[], long Nbr2[], long Sum[], int NumberLength) {
		long carry = 0;
		for (int i = 0; i < NumberLength; i++) {
			carry = (carry >> 32) + Nbr1[i] + Nbr2[i];
			Sum[i] = carry & 0xFFFFFFFFL;
		}
	}

	private void SubtractBigNbr32(long Nbr1[], long Nbr2[], long Diff[], int NumberLength) {
		long carry = 0;
		for (int i = 0; i < NumberLength; i++) {
			carry = (carry >> 32) + Nbr1[i] - Nbr2[i];
			Diff[i] = carry & 0xFFFFFFFFL;
		}
	}

	private static void AddBigNbrModN(int Nbr1[], int Nbr2[], int Sum[], int TestNbr[], int NumberLength) {
		long MaxUInt = 0x7FFFFFFFL;
		long carry = 0;
		int i;

		for (i = 0; i < NumberLength; i++) {
			carry = (carry >> 31) + (long) Nbr1[i] + (long) Nbr2[i] - (long) TestNbr[i];
			Sum[i] = (int) (carry & MaxUInt);
		}
		if (carry < 0) {
			carry = 0;
			for (i = 0; i < NumberLength; i++) {
				carry = (carry >> 31) + (long) Sum[i] + (long) TestNbr[i];
				Sum[i] = (int) (carry & MaxUInt);
			}
		}
	}

	private static void SubtractBigNbrModN(int Nbr1[], int Nbr2[], int Diff[], int TestNbr[], int NumberLength) {
		long MaxUInt = 0x7FFFFFFFL;
		long carry = 0;
		int i;

		for (i = 0; i < NumberLength; i++) {
			carry = (carry >> 31) + (long) Nbr1[i] - (long) Nbr2[i];
			Diff[i] = (int) (carry & MaxUInt);
		}
		if (carry < 0) {
			carry = 0;
			for (i = 0; i < NumberLength; i++) {
				carry = (carry >> 31) + (long) Diff[i] + (long) TestNbr[i];
				Diff[i] = (int) (carry & MaxUInt);
			}
		}
	}

	private void MontgomeryMult(int Nbr1[], int Nbr2[], int Prod[]) {
		int NumberLength = this.NumberLength;

		switch (NumberLength) {
		case 2:
			MontgomeryMult2(Nbr1, Nbr2, Prod);
			break;
		case 3:
			MontgomeryMult3(Nbr1, Nbr2, Prod);
			break;
		case 4:
			MontgomeryMult4(Nbr1, Nbr2, Prod);
			break;
		case 5:
			MontgomeryMult5(Nbr1, Nbr2, Prod);
			break;
		case 6:
			MontgomeryMult6(Nbr1, Nbr2, Prod);
			break;
		case 7:
			MontgomeryMult7(Nbr1, Nbr2, Prod);
			break;
		case 8:
			MontgomeryMult8(Nbr1, Nbr2, Prod);
			break;
		case 9:
			MontgomeryMult9(Nbr1, Nbr2, Prod);
			break;
		case 10:
			MontgomeryMult10(Nbr1, Nbr2, Prod);
			break;
		case 11:
			MontgomeryMult11(Nbr1, Nbr2, Prod);
			break;
		default:
			LargeMontgomeryMult(Nbr1, Nbr2, Prod);
			break;
		}
	}

	private void MontgomeryMult2(int Nbr1[], int Nbr2[], int Prod[]) {
		long Pr, Nbr, MontDig;
		long Prod0, Prod1;
		Prod0 = Prod1 = 0;
		long TestNbr0 = TestNbr[0];
		long TestNbr1 = TestNbr[1];
		long Nbr2_0 = Nbr2[0];
		long Nbr2_1 = Nbr2[1];
		for (int i = 0; i < 2; i++) {
			Pr = (Nbr = Nbr1[i]) * Nbr2_0 + Prod0;
			MontDig = ((int) Pr * MontgomeryMultN) & 0x7FFFFFFFL;
			Prod0 = (Pr = ((MontDig * TestNbr0 + Pr) >>> 31) + MontDig * TestNbr1 + Nbr * Nbr2_1 + Prod1) & 0x7FFFFFFFL;
			Prod1 = Pr >>> 31;
		}
		if (Prod1 > TestNbr1 || Prod1 == TestNbr1 && (Prod0 >= TestNbr0)) {
			Prod0 = (Pr = Prod0 - TestNbr0) & 0x7FFFFFFFL;
			Prod1 = ((Pr >> 31) + Prod1 - TestNbr1) & 0x7FFFFFFFL;
		}
		Prod[0] = (int) Prod0;
		Prod[1] = (int) Prod1;
	}

	private void MontgomeryMult3(int Nbr1[], int Nbr2[], int Prod[]) {
		long Pr, Nbr, MontDig;
		long Prod0, Prod1, Prod2;
		Prod0 = Prod1 = Prod2 = 0;
		long TestNbr0 = TestNbr[0];
		long TestNbr1 = TestNbr[1];
		long TestNbr2 = TestNbr[2];
		long Nbr2_0 = Nbr2[0];
		long Nbr2_1 = Nbr2[1];
		long Nbr2_2 = Nbr2[2];
		for (int i = 0; i < 3; i++) {
			Pr = (Nbr = Nbr1[i]) * Nbr2_0 + Prod0;
			MontDig = ((int) Pr * MontgomeryMultN) & 0x7FFFFFFFL;
			Prod0 = (Pr = ((MontDig * TestNbr0 + Pr) >>> 31) + MontDig * TestNbr1 + Nbr * Nbr2_1 + Prod1) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >>> 31) + MontDig * TestNbr2 + Nbr * Nbr2_2 + Prod2) & 0x7FFFFFFFL;
			Prod2 = Pr >>> 31;
		}
		if (Prod2 > TestNbr2 || Prod2 == TestNbr2 && (Prod1 > TestNbr1 || Prod1 == TestNbr1 && (Prod0 >= TestNbr0))) {
			Prod0 = (Pr = Prod0 - TestNbr0) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >> 31) + Prod1 - TestNbr1) & 0x7FFFFFFFL;
			Prod2 = ((Pr >> 31) + Prod2 - TestNbr2) & 0x7FFFFFFFL;
		}
		Prod[0] = (int) Prod0;
		Prod[1] = (int) Prod1;
		Prod[2] = (int) Prod2;
	}

	private void MontgomeryMult4(int Nbr1[], int Nbr2[], int Prod[]) {
		long Pr, Nbr, MontDig;
		long Prod0, Prod1, Prod2, Prod3;
		Prod0 = Prod1 = Prod2 = Prod3 = 0;
		long TestNbr0 = TestNbr[0];
		long TestNbr1 = TestNbr[1];
		long TestNbr2 = TestNbr[2];
		long TestNbr3 = TestNbr[3];
		long Nbr2_0 = Nbr2[0];
		long Nbr2_1 = Nbr2[1];
		long Nbr2_2 = Nbr2[2];
		long Nbr2_3 = Nbr2[3];
		for (int i = 0; i < 4; i++) {
			Pr = (Nbr = Nbr1[i]) * Nbr2_0 + Prod0;
			MontDig = ((int) Pr * MontgomeryMultN) & 0x7FFFFFFFL;
			Prod0 = (Pr = ((MontDig * TestNbr0 + Pr) >>> 31) + MontDig * TestNbr1 + Nbr * Nbr2_1 + Prod1) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >>> 31) + MontDig * TestNbr2 + Nbr * Nbr2_2 + Prod2) & 0x7FFFFFFFL;
			Prod2 = (Pr = (Pr >>> 31) + MontDig * TestNbr3 + Nbr * Nbr2_3 + Prod3) & 0x7FFFFFFFL;
			Prod3 = Pr >>> 31;
		}
		if (Prod3 > TestNbr3 || Prod3 == TestNbr3 && (Prod2 > TestNbr2 || Prod2 == TestNbr2 && (Prod1 > TestNbr1 || Prod1 == TestNbr1 && (Prod0 >= TestNbr0)))) {
			Prod0 = (Pr = Prod0 - TestNbr0) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >> 31) + Prod1 - TestNbr1) & 0x7FFFFFFFL;
			Prod2 = (Pr = (Pr >> 31) + Prod2 - TestNbr2) & 0x7FFFFFFFL;
			Prod3 = ((Pr >> 31) + Prod3 - TestNbr3) & 0x7FFFFFFFL;
		}
		Prod[0] = (int) Prod0;
		Prod[1] = (int) Prod1;
		Prod[2] = (int) Prod2;
		Prod[3] = (int) Prod3;
	}

	private void MontgomeryMult5(int Nbr1[], int Nbr2[], int Prod[]) {
		long Pr, Nbr, MontDig;
		long Prod0, Prod1, Prod2, Prod3, Prod4;
		Prod0 = Prod1 = Prod2 = Prod3 = Prod4 = 0;
		long TestNbr0 = TestNbr[0];
		long TestNbr1 = TestNbr[1];
		long TestNbr2 = TestNbr[2];
		long TestNbr3 = TestNbr[3];
		long TestNbr4 = TestNbr[4];
		long Nbr2_0 = Nbr2[0];
		long Nbr2_1 = Nbr2[1];
		long Nbr2_2 = Nbr2[2];
		long Nbr2_3 = Nbr2[3];
		long Nbr2_4 = Nbr2[4];
		for (int i = 0; i < 5; i++) {
			Pr = (Nbr = Nbr1[i]) * Nbr2_0 + Prod0;
			MontDig = ((int) Pr * MontgomeryMultN) & 0x7FFFFFFFL;
			Prod0 = (Pr = ((MontDig * TestNbr0 + Pr) >>> 31) + MontDig * TestNbr1 + Nbr * Nbr2_1 + Prod1) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >>> 31) + MontDig * TestNbr2 + Nbr * Nbr2_2 + Prod2) & 0x7FFFFFFFL;
			Prod2 = (Pr = (Pr >>> 31) + MontDig * TestNbr3 + Nbr * Nbr2_3 + Prod3) & 0x7FFFFFFFL;
			Prod3 = (Pr = (Pr >>> 31) + MontDig * TestNbr4 + Nbr * Nbr2_4 + Prod4) & 0x7FFFFFFFL;
			Prod4 = Pr >>> 31;
		}
		if (Prod4 > TestNbr4
				|| Prod4 == TestNbr4
				&& (Prod3 > TestNbr3 || Prod3 == TestNbr3
						&& (Prod2 > TestNbr2 || Prod2 == TestNbr2 && (Prod1 > TestNbr1 || Prod1 == TestNbr1 && (Prod0 >= TestNbr0))))) {
			Prod0 = (Pr = Prod0 - TestNbr0) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >> 31) + Prod1 - TestNbr1) & 0x7FFFFFFFL;
			Prod2 = (Pr = (Pr >> 31) + Prod2 - TestNbr2) & 0x7FFFFFFFL;
			Prod3 = (Pr = (Pr >> 31) + Prod3 - TestNbr3) & 0x7FFFFFFFL;
			Prod4 = ((Pr >> 31) + Prod4 - TestNbr4) & 0x7FFFFFFFL;
		}
		Prod[0] = (int) Prod0;
		Prod[1] = (int) Prod1;
		Prod[2] = (int) Prod2;
		Prod[3] = (int) Prod3;
		Prod[4] = (int) Prod4;
	}

	private void MontgomeryMult6(int Nbr1[], int Nbr2[], int Prod[]) {
		long Pr, Nbr, MontDig;
		long Prod0, Prod1, Prod2, Prod3, Prod4, Prod5;
		Prod0 = Prod1 = Prod2 = Prod3 = Prod4 = Prod5 = 0;
		long TestNbr0 = TestNbr[0];
		long TestNbr1 = TestNbr[1];
		long TestNbr2 = TestNbr[2];
		long TestNbr3 = TestNbr[3];
		long TestNbr4 = TestNbr[4];
		long TestNbr5 = TestNbr[5];
		long Nbr2_0 = Nbr2[0];
		long Nbr2_1 = Nbr2[1];
		long Nbr2_2 = Nbr2[2];
		long Nbr2_3 = Nbr2[3];
		long Nbr2_4 = Nbr2[4];
		long Nbr2_5 = Nbr2[5];
		for (int i = 0; i < 6; i++) {
			Pr = (Nbr = Nbr1[i]) * Nbr2_0 + Prod0;
			MontDig = ((int) Pr * MontgomeryMultN) & 0x7FFFFFFFL;
			Prod0 = (Pr = ((MontDig * TestNbr0 + Pr) >>> 31) + MontDig * TestNbr1 + Nbr * Nbr2_1 + Prod1) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >>> 31) + MontDig * TestNbr2 + Nbr * Nbr2_2 + Prod2) & 0x7FFFFFFFL;
			Prod2 = (Pr = (Pr >>> 31) + MontDig * TestNbr3 + Nbr * Nbr2_3 + Prod3) & 0x7FFFFFFFL;
			Prod3 = (Pr = (Pr >>> 31) + MontDig * TestNbr4 + Nbr * Nbr2_4 + Prod4) & 0x7FFFFFFFL;
			Prod4 = (Pr = (Pr >>> 31) + MontDig * TestNbr5 + Nbr * Nbr2_5 + Prod5) & 0x7FFFFFFFL;
			Prod5 = Pr >>> 31;
		}
		if (Prod5 > TestNbr5
				|| Prod5 == TestNbr5
				&& (Prod4 > TestNbr4 || Prod4 == TestNbr4
						&& (Prod3 > TestNbr3 || Prod3 == TestNbr3
								&& (Prod2 > TestNbr2 || Prod2 == TestNbr2 && (Prod1 > TestNbr1 || Prod1 == TestNbr1 && (Prod0 >= TestNbr0)))))) {
			Prod0 = (Pr = Prod0 - TestNbr0) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >> 31) + Prod1 - TestNbr1) & 0x7FFFFFFFL;
			Prod2 = (Pr = (Pr >> 31) + Prod2 - TestNbr2) & 0x7FFFFFFFL;
			Prod3 = (Pr = (Pr >> 31) + Prod3 - TestNbr3) & 0x7FFFFFFFL;
			Prod4 = (Pr = (Pr >> 31) + Prod4 - TestNbr4) & 0x7FFFFFFFL;
			Prod5 = ((Pr >> 31) + Prod5 - TestNbr5) & 0x7FFFFFFFL;
		}
		Prod[0] = (int) Prod0;
		Prod[1] = (int) Prod1;
		Prod[2] = (int) Prod2;
		Prod[3] = (int) Prod3;
		Prod[4] = (int) Prod4;
		Prod[5] = (int) Prod5;
	}

	private void MontgomeryMult7(int Nbr1[], int Nbr2[], int Prod[]) {
		long Pr, Nbr;
		int MontDig;
		int Prod0, Prod1, Prod2, Prod3, Prod4, Prod5, Prod6;
		Prod0 = Prod1 = Prod2 = Prod3 = Prod4 = Prod5 = Prod6 = 0;
		int TestNbr0 = (int) TestNbr[0];
		int TestNbr1 = (int) TestNbr[1];
		int TestNbr2 = (int) TestNbr[2];
		int TestNbr3 = (int) TestNbr[3];
		int TestNbr4 = (int) TestNbr[4];
		int TestNbr5 = (int) TestNbr[5];
		int TestNbr6 = (int) TestNbr[6];
		int Nbr2_0 = (int) Nbr2[0];
		int Nbr2_1 = (int) Nbr2[1];
		int Nbr2_2 = (int) Nbr2[2];
		int Nbr2_3 = (int) Nbr2[3];
		int Nbr2_4 = (int) Nbr2[4];
		int Nbr2_5 = (int) Nbr2[5];
		int Nbr2_6 = (int) Nbr2[6];
		int Sum;
		for (int i = 0; i < 7; i++) {
			Pr = (Nbr = Nbr1[i]) * (long) Nbr2_0 + Prod0;
			MontDig = ((int) Pr * (int) MontgomeryMultN) & 0x7FFFFFFF;
			Prod0 = (int) (Pr = ((MontDig * (long) TestNbr0 + Pr) >>> 31) + MontDig * (long) TestNbr1 + Nbr * (long) Nbr2_1 + Prod1) & 0x7FFFFFFF;
			Prod1 = (int) (Pr = (Pr >>> 31) + MontDig * (long) TestNbr2 + Nbr * (long) Nbr2_2 + Prod2) & 0x7FFFFFFF;
			Prod2 = (int) (Pr = (Pr >>> 31) + MontDig * (long) TestNbr3 + Nbr * (long) Nbr2_3 + Prod3) & 0x7FFFFFFF;
			Prod3 = (int) (Pr = (Pr >>> 31) + MontDig * (long) TestNbr4 + Nbr * (long) Nbr2_4 + Prod4) & 0x7FFFFFFF;
			Prod4 = (int) (Pr = (Pr >>> 31) + MontDig * (long) TestNbr5 + Nbr * (long) Nbr2_5 + Prod5) & 0x7FFFFFFF;
			Prod5 = (int) (Pr = (Pr >>> 31) + MontDig * (long) TestNbr6 + Nbr * (long) Nbr2_6 + Prod6) & 0x7FFFFFFF;
			Prod6 = (int) (Pr >>> 31);
		}
		if (Prod6 > TestNbr6
				|| Prod6 == TestNbr6
				&& (Prod5 > TestNbr5 || Prod5 == TestNbr5
						&& (Prod4 > TestNbr4 || Prod4 == TestNbr4
								&& (Prod3 > TestNbr3 || Prod3 == TestNbr3
										&& (Prod2 > TestNbr2 || Prod2 == TestNbr2 && (Prod1 > TestNbr1 || Prod1 == TestNbr1 && (Prod0 >= TestNbr0))))))) {
			Prod0 = (Sum = Prod0 - TestNbr0) & 0x7FFFFFFF;
			Prod1 = (Sum = (Sum >> 31) + (Prod1 - TestNbr1)) & 0x7FFFFFFF;
			Prod2 = (Sum = (Sum >> 31) + (Prod2 - TestNbr2)) & 0x7FFFFFFF;
			Prod3 = (Sum = (Sum >> 31) + (Prod3 - TestNbr3)) & 0x7FFFFFFF;
			Prod4 = (Sum = (Sum >> 31) + (Prod4 - TestNbr4)) & 0x7FFFFFFF;
			Prod5 = (Sum = (Sum >> 31) + (Prod5 - TestNbr5)) & 0x7FFFFFFF;
			Prod6 = ((Sum >> 31) + (Prod6 - TestNbr6)) & 0x7FFFFFFF;
		}
		Prod[0] = (int) Prod0;
		Prod[1] = (int) Prod1;
		Prod[2] = (int) Prod2;
		Prod[3] = (int) Prod3;
		Prod[4] = (int) Prod4;
		Prod[5] = (int) Prod5;
		Prod[6] = (int) Prod6;
	}

	private void MontgomeryMult8(int Nbr1[], int Nbr2[], int Prod[]) {
		long Pr, Nbr, MontDig;
		long Prod0, Prod1, Prod2, Prod3, Prod4, Prod5, Prod6, Prod7;
		Prod0 = Prod1 = Prod2 = Prod3 = Prod4 = Prod5 = Prod6 = Prod7 = 0;
		long TestNbr0 = TestNbr[0];
		long TestNbr1 = TestNbr[1];
		long TestNbr2 = TestNbr[2];
		long TestNbr3 = TestNbr[3];
		long TestNbr4 = TestNbr[4];
		long TestNbr5 = TestNbr[5];
		long TestNbr6 = TestNbr[6];
		long TestNbr7 = TestNbr[7];
		long Nbr2_0 = Nbr2[0];
		long Nbr2_1 = Nbr2[1];
		long Nbr2_2 = Nbr2[2];
		long Nbr2_3 = Nbr2[3];
		long Nbr2_4 = Nbr2[4];
		long Nbr2_5 = Nbr2[5];
		long Nbr2_6 = Nbr2[6];
		long Nbr2_7 = Nbr2[7];
		for (int i = 0; i < 8; i++) {
			Pr = (Nbr = Nbr1[i]) * Nbr2_0 + Prod0;
			MontDig = ((int) Pr * MontgomeryMultN) & 0x7FFFFFFFL;
			Prod0 = (Pr = ((MontDig * TestNbr0 + Pr) >>> 31) + MontDig * TestNbr1 + Nbr * Nbr2_1 + Prod1) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >>> 31) + MontDig * TestNbr2 + Nbr * Nbr2_2 + Prod2) & 0x7FFFFFFFL;
			Prod2 = (Pr = (Pr >>> 31) + MontDig * TestNbr3 + Nbr * Nbr2_3 + Prod3) & 0x7FFFFFFFL;
			Prod3 = (Pr = (Pr >>> 31) + MontDig * TestNbr4 + Nbr * Nbr2_4 + Prod4) & 0x7FFFFFFFL;
			Prod4 = (Pr = (Pr >>> 31) + MontDig * TestNbr5 + Nbr * Nbr2_5 + Prod5) & 0x7FFFFFFFL;
			Prod5 = (Pr = (Pr >>> 31) + MontDig * TestNbr6 + Nbr * Nbr2_6 + Prod6) & 0x7FFFFFFFL;
			Prod6 = (Pr = (Pr >>> 31) + MontDig * TestNbr7 + Nbr * Nbr2_7 + Prod7) & 0x7FFFFFFFL;
			Prod7 = Pr >>> 31;
		}
		if (Prod7 > TestNbr7
				|| Prod7 == TestNbr7
				&& (Prod6 > TestNbr6 || Prod6 == TestNbr6
						&& (Prod5 > TestNbr5 || Prod5 == TestNbr5
								&& (Prod4 > TestNbr4 || Prod4 == TestNbr4
										&& (Prod3 > TestNbr3 || Prod3 == TestNbr3
												&& (Prod2 > TestNbr2 || Prod2 == TestNbr2 && (Prod1 > TestNbr1 || Prod1 == TestNbr1 && (Prod0 >= TestNbr0)))))))) {
			Prod[0] = (int) ((Pr = Prod0 - TestNbr0) & 0x7FFFFFFFL);
			Prod[1] = (int) ((Pr = (Pr >> 31) + Prod1 - TestNbr1) & 0x7FFFFFFFL);
			Prod[2] = (int) ((Pr = (Pr >> 31) + Prod2 - TestNbr2) & 0x7FFFFFFFL);
			Prod[3] = (int) ((Pr = (Pr >> 31) + Prod3 - TestNbr3) & 0x7FFFFFFFL);
			Prod[4] = (int) ((Pr = (Pr >> 31) + Prod4 - TestNbr4) & 0x7FFFFFFFL);
			Prod[5] = (int) ((Pr = (Pr >> 31) + Prod5 - TestNbr5) & 0x7FFFFFFFL);
			Prod[6] = (int) ((Pr = (Pr >> 31) + Prod6 - TestNbr6) & 0x7FFFFFFFL);
			Prod[7] = (int) (((Pr >> 31) + Prod7 - TestNbr7) & 0x7FFFFFFFL);
			return;
		}
		Prod[0] = (int) Prod0;
		Prod[1] = (int) Prod1;
		Prod[2] = (int) Prod2;
		Prod[3] = (int) Prod3;
		Prod[4] = (int) Prod4;
		Prod[5] = (int) Prod5;
		Prod[6] = (int) Prod6;
		Prod[7] = (int) Prod7;
	}

	private void MontgomeryMult9(int Nbr1[], int Nbr2[], int Prod[]) {
		long Pr, Nbr, MontDig;
		long Prod0, Prod1, Prod2, Prod3, Prod4, Prod5, Prod6, Prod7, Prod8;
		Prod0 = Prod1 = Prod2 = Prod3 = Prod4 = Prod5 = Prod6 = Prod7 = Prod8 = 0;
		long TestNbr0 = TestNbr[0];
		long TestNbr1 = TestNbr[1];
		long TestNbr2 = TestNbr[2];
		long TestNbr3 = TestNbr[3];
		long TestNbr4 = TestNbr[4];
		long TestNbr5 = TestNbr[5];
		long TestNbr6 = TestNbr[6];
		long TestNbr7 = TestNbr[7];
		long TestNbr8 = TestNbr[8];
		long Nbr2_0 = Nbr2[0];
		long Nbr2_1 = Nbr2[1];
		long Nbr2_2 = Nbr2[2];
		long Nbr2_3 = Nbr2[3];
		long Nbr2_4 = Nbr2[4];
		long Nbr2_5 = Nbr2[5];
		long Nbr2_6 = Nbr2[6];
		long Nbr2_7 = Nbr2[7];
		long Nbr2_8 = Nbr2[8];
		for (int i = 0; i < 9; i++) {
			Pr = (Nbr = Nbr1[i]) * Nbr2_0 + Prod0;
			MontDig = ((int) Pr * MontgomeryMultN) & 0x7FFFFFFFL;
			Prod0 = (Pr = ((MontDig * TestNbr0 + Pr) >>> 31) + MontDig * TestNbr1 + Nbr * Nbr2_1 + Prod1) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >>> 31) + MontDig * TestNbr2 + Nbr * Nbr2_2 + Prod2) & 0x7FFFFFFFL;
			Prod2 = (Pr = (Pr >>> 31) + MontDig * TestNbr3 + Nbr * Nbr2_3 + Prod3) & 0x7FFFFFFFL;
			Prod3 = (Pr = (Pr >>> 31) + MontDig * TestNbr4 + Nbr * Nbr2_4 + Prod4) & 0x7FFFFFFFL;
			Prod4 = (Pr = (Pr >>> 31) + MontDig * TestNbr5 + Nbr * Nbr2_5 + Prod5) & 0x7FFFFFFFL;
			Prod5 = (Pr = (Pr >>> 31) + MontDig * TestNbr6 + Nbr * Nbr2_6 + Prod6) & 0x7FFFFFFFL;
			Prod6 = (Pr = (Pr >>> 31) + MontDig * TestNbr7 + Nbr * Nbr2_7 + Prod7) & 0x7FFFFFFFL;
			Prod7 = (Pr = (Pr >>> 31) + MontDig * TestNbr8 + Nbr * Nbr2_8 + Prod8) & 0x7FFFFFFFL;
			Prod8 = Pr >>> 31;
		}
		if (Prod8 > TestNbr8
				|| Prod8 == TestNbr8
				&& (Prod7 > TestNbr7 || Prod7 == TestNbr7
						&& (Prod6 > TestNbr6 || Prod6 == TestNbr6
								&& (Prod5 > TestNbr5 || Prod5 == TestNbr5
										&& (Prod4 > TestNbr4 || Prod4 == TestNbr4
												&& (Prod3 > TestNbr3 || Prod3 == TestNbr3
														&& (Prod2 > TestNbr2 || Prod2 == TestNbr2
																&& (Prod1 > TestNbr1 || Prod1 == TestNbr1 && (Prod0 >= TestNbr0))))))))) {
			Prod0 = (Pr = Prod0 - TestNbr0) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >> 31) + Prod1 - TestNbr1) & 0x7FFFFFFFL;
			Prod2 = (Pr = (Pr >> 31) + Prod2 - TestNbr2) & 0x7FFFFFFFL;
			Prod3 = (Pr = (Pr >> 31) + Prod3 - TestNbr3) & 0x7FFFFFFFL;
			Prod4 = (Pr = (Pr >> 31) + Prod4 - TestNbr4) & 0x7FFFFFFFL;
			Prod5 = (Pr = (Pr >> 31) + Prod5 - TestNbr5) & 0x7FFFFFFFL;
			Prod6 = (Pr = (Pr >> 31) + Prod6 - TestNbr6) & 0x7FFFFFFFL;
			Prod7 = (Pr = (Pr >> 31) + Prod7 - TestNbr7) & 0x7FFFFFFFL;
			Prod8 = ((Pr >> 31) + Prod8 - TestNbr8) & 0x7FFFFFFFL;
		}
		Prod[0] = (int) Prod0;
		Prod[1] = (int) Prod1;
		Prod[2] = (int) Prod2;
		Prod[3] = (int) Prod3;
		Prod[4] = (int) Prod4;
		Prod[5] = (int) Prod5;
		Prod[6] = (int) Prod6;
		Prod[7] = (int) Prod7;
		Prod[8] = (int) Prod8;
	}

	private void MontgomeryMult10(int Nbr1[], int Nbr2[], int Prod[]) {
		long Pr, Nbr, MontDig;
		long Prod0, Prod1, Prod2, Prod3, Prod4, Prod5, Prod6, Prod7, Prod8, Prod9;
		Prod0 = Prod1 = Prod2 = Prod3 = Prod4 = Prod5 = Prod6 = Prod7 = Prod8 = Prod9 = 0;
		long TestNbr0 = TestNbr[0];
		long TestNbr1 = TestNbr[1];
		long TestNbr2 = TestNbr[2];
		long TestNbr3 = TestNbr[3];
		long TestNbr4 = TestNbr[4];
		long TestNbr5 = TestNbr[5];
		long TestNbr6 = TestNbr[6];
		long TestNbr7 = TestNbr[7];
		long TestNbr8 = TestNbr[8];
		long TestNbr9 = TestNbr[9];
		long Nbr2_0 = Nbr2[0];
		long Nbr2_1 = Nbr2[1];
		long Nbr2_2 = Nbr2[2];
		long Nbr2_3 = Nbr2[3];
		long Nbr2_4 = Nbr2[4];
		long Nbr2_5 = Nbr2[5];
		long Nbr2_6 = Nbr2[6];
		long Nbr2_7 = Nbr2[7];
		long Nbr2_8 = Nbr2[8];
		long Nbr2_9 = Nbr2[9];
		for (int i = 0; i < 10; i++) {
			Pr = (Nbr = Nbr1[i]) * Nbr2_0 + Prod0;
			MontDig = ((int) Pr * MontgomeryMultN) & 0x7FFFFFFFL;
			Prod0 = (Pr = ((MontDig * TestNbr0 + Pr) >>> 31) + MontDig * TestNbr1 + Nbr * Nbr2_1 + Prod1) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >>> 31) + MontDig * TestNbr2 + Nbr * Nbr2_2 + Prod2) & 0x7FFFFFFFL;
			Prod2 = (Pr = (Pr >>> 31) + MontDig * TestNbr3 + Nbr * Nbr2_3 + Prod3) & 0x7FFFFFFFL;
			Prod3 = (Pr = (Pr >>> 31) + MontDig * TestNbr4 + Nbr * Nbr2_4 + Prod4) & 0x7FFFFFFFL;
			Prod4 = (Pr = (Pr >>> 31) + MontDig * TestNbr5 + Nbr * Nbr2_5 + Prod5) & 0x7FFFFFFFL;
			Prod5 = (Pr = (Pr >>> 31) + MontDig * TestNbr6 + Nbr * Nbr2_6 + Prod6) & 0x7FFFFFFFL;
			Prod6 = (Pr = (Pr >>> 31) + MontDig * TestNbr7 + Nbr * Nbr2_7 + Prod7) & 0x7FFFFFFFL;
			Prod7 = (Pr = (Pr >>> 31) + MontDig * TestNbr8 + Nbr * Nbr2_8 + Prod8) & 0x7FFFFFFFL;
			Prod8 = (Pr = (Pr >>> 31) + MontDig * TestNbr9 + Nbr * Nbr2_9 + Prod9) & 0x7FFFFFFFL;
			Prod9 = Pr >>> 31;
		}
		if (Prod9 > TestNbr9
				|| Prod9 == TestNbr9
				&& (Prod8 > TestNbr8 || Prod8 == TestNbr8
						&& (Prod7 > TestNbr7 || Prod7 == TestNbr7
								&& (Prod6 > TestNbr6 || Prod6 == TestNbr6
										&& (Prod5 > TestNbr5 || Prod5 == TestNbr5
												&& (Prod4 > TestNbr4 || Prod4 == TestNbr4
														&& (Prod3 > TestNbr3 || Prod3 == TestNbr3
																&& (Prod2 > TestNbr2 || Prod2 == TestNbr2
																		&& (Prod1 > TestNbr1 || Prod1 == TestNbr1 && (Prod0 >= TestNbr0)))))))))) {
			Prod0 = (Pr = Prod0 - TestNbr0) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >> 31) + Prod1 - TestNbr1) & 0x7FFFFFFFL;
			Prod2 = (Pr = (Pr >> 31) + Prod2 - TestNbr2) & 0x7FFFFFFFL;
			Prod3 = (Pr = (Pr >> 31) + Prod3 - TestNbr3) & 0x7FFFFFFFL;
			Prod4 = (Pr = (Pr >> 31) + Prod4 - TestNbr4) & 0x7FFFFFFFL;
			Prod5 = (Pr = (Pr >> 31) + Prod5 - TestNbr5) & 0x7FFFFFFFL;
			Prod6 = (Pr = (Pr >> 31) + Prod6 - TestNbr6) & 0x7FFFFFFFL;
			Prod7 = (Pr = (Pr >> 31) + Prod7 - TestNbr7) & 0x7FFFFFFFL;
			Prod8 = (Pr = (Pr >> 31) + Prod8 - TestNbr8) & 0x7FFFFFFFL;
			Prod9 = ((Pr >> 31) + Prod9 - TestNbr9) & 0x7FFFFFFFL;
		}
		Prod[0] = (int) Prod0;
		Prod[1] = (int) Prod1;
		Prod[2] = (int) Prod2;
		Prod[3] = (int) Prod3;
		Prod[4] = (int) Prod4;
		Prod[5] = (int) Prod5;
		Prod[6] = (int) Prod6;
		Prod[7] = (int) Prod7;
		Prod[8] = (int) Prod8;
		Prod[9] = (int) Prod9;
	}

	private void MontgomeryMult11(int Nbr1[], int Nbr2[], int Prod[]) {
		long Pr, Nbr, MontDig;
		long Prod0, Prod1, Prod2, Prod3, Prod4, Prod5, Prod6, Prod7, Prod8, Prod9, Prod10;
		Prod0 = Prod1 = Prod2 = Prod3 = Prod4 = Prod5 = Prod6 = Prod7 = Prod8 = Prod9 = Prod10 = 0;
		long TestNbr0 = TestNbr[0];
		long TestNbr1 = TestNbr[1];
		long TestNbr2 = TestNbr[2];
		long TestNbr3 = TestNbr[3];
		long TestNbr4 = TestNbr[4];
		long TestNbr5 = TestNbr[5];
		long TestNbr6 = TestNbr[6];
		long TestNbr7 = TestNbr[7];
		long TestNbr8 = TestNbr[8];
		long TestNbr9 = TestNbr[9];
		long TestNbr10 = TestNbr[10];
		long Nbr2_0 = Nbr2[0];
		long Nbr2_1 = Nbr2[1];
		long Nbr2_2 = Nbr2[2];
		long Nbr2_3 = Nbr2[3];
		long Nbr2_4 = Nbr2[4];
		long Nbr2_5 = Nbr2[5];
		long Nbr2_6 = Nbr2[6];
		long Nbr2_7 = Nbr2[7];
		long Nbr2_8 = Nbr2[8];
		long Nbr2_9 = Nbr2[9];
		long Nbr2_10 = Nbr2[10];
		for (int i = 0; i < 11; i++) {
			Pr = (Nbr = Nbr1[i]) * Nbr2_0 + Prod0;
			MontDig = ((int) Pr * MontgomeryMultN) & 0x7FFFFFFFL;
			Prod0 = (Pr = ((MontDig * TestNbr0 + Pr) >>> 31) + MontDig * TestNbr1 + Nbr * Nbr2_1 + Prod1) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >>> 31) + MontDig * TestNbr2 + Nbr * Nbr2_2 + Prod2) & 0x7FFFFFFFL;
			Prod2 = (Pr = (Pr >>> 31) + MontDig * TestNbr3 + Nbr * Nbr2_3 + Prod3) & 0x7FFFFFFFL;
			Prod3 = (Pr = (Pr >>> 31) + MontDig * TestNbr4 + Nbr * Nbr2_4 + Prod4) & 0x7FFFFFFFL;
			Prod4 = (Pr = (Pr >>> 31) + MontDig * TestNbr5 + Nbr * Nbr2_5 + Prod5) & 0x7FFFFFFFL;
			Prod5 = (Pr = (Pr >>> 31) + MontDig * TestNbr6 + Nbr * Nbr2_6 + Prod6) & 0x7FFFFFFFL;
			Prod6 = (Pr = (Pr >>> 31) + MontDig * TestNbr7 + Nbr * Nbr2_7 + Prod7) & 0x7FFFFFFFL;
			Prod7 = (Pr = (Pr >>> 31) + MontDig * TestNbr8 + Nbr * Nbr2_8 + Prod8) & 0x7FFFFFFFL;
			Prod8 = (Pr = (Pr >>> 31) + MontDig * TestNbr9 + Nbr * Nbr2_9 + Prod9) & 0x7FFFFFFFL;
			Prod9 = (Pr = (Pr >>> 31) + MontDig * TestNbr10 + Nbr * Nbr2_10 + Prod10) & 0x7FFFFFFFL;
			Prod10 = Pr >>> 31;
		}
		if (Prod10 > TestNbr10
				|| Prod10 == TestNbr10
				&& (Prod9 > TestNbr9 || Prod9 == TestNbr9
						&& (Prod8 > TestNbr8 || Prod8 == TestNbr8
								&& (Prod7 > TestNbr7 || Prod7 == TestNbr7
										&& (Prod6 > TestNbr6 || Prod6 == TestNbr6
												&& (Prod5 > TestNbr5 || Prod5 == TestNbr5
														&& (Prod4 > TestNbr4 || Prod4 == TestNbr4
																&& (Prod3 > TestNbr3 || Prod3 == TestNbr3
																		&& (Prod2 > TestNbr2 || Prod2 == TestNbr2
																				&& (Prod1 > TestNbr1 || Prod1 == TestNbr1 && (Prod0 >= TestNbr0))))))))))) {
			Prod0 = (Pr = Prod0 - TestNbr0) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >> 31) + Prod1 - TestNbr1) & 0x7FFFFFFFL;
			Prod2 = (Pr = (Pr >> 31) + Prod2 - TestNbr2) & 0x7FFFFFFFL;
			Prod3 = (Pr = (Pr >> 31) + Prod3 - TestNbr3) & 0x7FFFFFFFL;
			Prod4 = (Pr = (Pr >> 31) + Prod4 - TestNbr4) & 0x7FFFFFFFL;
			Prod5 = (Pr = (Pr >> 31) + Prod5 - TestNbr5) & 0x7FFFFFFFL;
			Prod6 = (Pr = (Pr >> 31) + Prod6 - TestNbr6) & 0x7FFFFFFFL;
			Prod7 = (Pr = (Pr >> 31) + Prod7 - TestNbr7) & 0x7FFFFFFFL;
			Prod8 = (Pr = (Pr >> 31) + Prod8 - TestNbr8) & 0x7FFFFFFFL;
			Prod9 = (Pr = (Pr >> 31) + Prod9 - TestNbr9) & 0x7FFFFFFFL;
			Prod10 = ((Pr >> 31) + Prod10 - TestNbr10) & 0x7FFFFFFFL;
		}
		Prod[0] = (int) Prod0;
		Prod[1] = (int) Prod1;
		Prod[2] = (int) Prod2;
		Prod[3] = (int) Prod3;
		Prod[4] = (int) Prod4;
		Prod[5] = (int) Prod5;
		Prod[6] = (int) Prod6;
		Prod[7] = (int) Prod7;
		Prod[8] = (int) Prod8;
		Prod[9] = (int) Prod9;
		Prod[10] = (int) Prod10;
	}

	private void LargeMontgomeryMult(int Nbr1[], int Nbr2[], int Prod[]) {
		long Pr, Nbr, MontDig;
		long Prod0, Prod1, Prod2, Prod3, Prod4, Prod5, Prod6, Prod7, Prod8, Prod9, Prod10;
		Prod0 = Prod1 = Prod2 = Prod3 = Prod4 = Prod5 = Prod6 = Prod7 = Prod8 = Prod9 = Prod10 = 0;
		long TestNbr0 = TestNbr[0];
		long TestNbr1 = TestNbr[1];
		long TestNbr2 = TestNbr[2];
		long TestNbr3 = TestNbr[3];
		long TestNbr4 = TestNbr[4];
		long TestNbr5 = TestNbr[5];
		long TestNbr6 = TestNbr[6];
		long TestNbr7 = TestNbr[7];
		long TestNbr8 = TestNbr[8];
		long TestNbr9 = TestNbr[9];
		long TestNbr10 = TestNbr[10];
		long Nbr2_0 = Nbr2[0];
		long Nbr2_1 = Nbr2[1];
		long Nbr2_2 = Nbr2[2];
		long Nbr2_3 = Nbr2[3];
		long Nbr2_4 = Nbr2[4];
		long Nbr2_5 = Nbr2[5];
		long Nbr2_6 = Nbr2[6];
		long Nbr2_7 = Nbr2[7];
		long Nbr2_8 = Nbr2[8];
		long Nbr2_9 = Nbr2[9];
		long Nbr2_10 = Nbr2[10];
		int j;
		for (j = 11; j < NumberLength; j++) {
			Prod[j] = 0;
		}
		for (int i = 0; i < NumberLength; i++) {
			Pr = (Nbr = Nbr1[i]) * Nbr2_0 + Prod0;
			MontDig = ((int) Pr * MontgomeryMultN) & 0x7FFFFFFFL;
			Prod0 = (Pr = ((MontDig * TestNbr0 + Pr) >>> 31) + MontDig * TestNbr1 + Nbr * Nbr2_1 + Prod1) & 0x7FFFFFFFL;
			Prod1 = (Pr = (Pr >>> 31) + MontDig * TestNbr2 + Nbr * Nbr2_2 + Prod2) & 0x7FFFFFFFL;
			Prod2 = (Pr = (Pr >>> 31) + MontDig * TestNbr3 + Nbr * Nbr2_3 + Prod3) & 0x7FFFFFFFL;
			Prod3 = (Pr = (Pr >>> 31) + MontDig * TestNbr4 + Nbr * Nbr2_4 + Prod4) & 0x7FFFFFFFL;
			Prod4 = (Pr = (Pr >>> 31) + MontDig * TestNbr5 + Nbr * Nbr2_5 + Prod5) & 0x7FFFFFFFL;
			Prod5 = (Pr = (Pr >>> 31) + MontDig * TestNbr6 + Nbr * Nbr2_6 + Prod6) & 0x7FFFFFFFL;
			Prod6 = (Pr = (Pr >>> 31) + MontDig * TestNbr7 + Nbr * Nbr2_7 + Prod7) & 0x7FFFFFFFL;
			Prod7 = (Pr = (Pr >>> 31) + MontDig * TestNbr8 + Nbr * Nbr2_8 + Prod8) & 0x7FFFFFFFL;
			Prod8 = (Pr = (Pr >>> 31) + MontDig * TestNbr9 + Nbr * Nbr2_9 + Prod9) & 0x7FFFFFFFL;
			Prod9 = (Pr = (Pr >>> 31) + MontDig * TestNbr10 + Nbr * Nbr2_10 + Prod10) & 0x7FFFFFFFL;
			Prod10 = (Pr = (Pr >>> 31) + MontDig * TestNbr[11] + Nbr * Nbr2[11] + Prod[11]) & 0x7FFFFFFFL;
			for (j = 12; j < NumberLength; j++) {
				Prod[j - 1] = (int) ((Pr = (Pr >>> 31) + MontDig * TestNbr[j] + Nbr * Nbr2[j] + Prod[j]) & 0x7FFFFFFFL);
			}
			Prod[j - 1] = (int) (Pr >>> 31);
		}
		Prod[0] = (int) Prod0;
		Prod[1] = (int) Prod1;
		Prod[2] = (int) Prod2;
		Prod[3] = (int) Prod3;
		Prod[4] = (int) Prod4;
		Prod[5] = (int) Prod5;
		Prod[6] = (int) Prod6;
		Prod[7] = (int) Prod7;
		Prod[8] = (int) Prod8;
		Prod[9] = (int) Prod9;
		Prod[10] = (int) Prod10;
		for (j = NumberLength - 1; j >= 0; j--) {
			if (Prod[j] != TestNbr[j]) {
				break;
			}
		}
		if (j < 0 || j >= 0 && Prod[j] >= TestNbr[j]) {
			Pr = 0;
			for (j = 0; j < NumberLength; j++) {
				Prod[j] = (int) ((Pr = (Pr >> 31) + (long) Prod[j] - (long) TestNbr[j]) & 0x7FFFFFFFL);
			}
		}
	}

	// Algorithm REDC: x is the product of both numbers
	// m = (x mod R) * N' mod R
	// t = (x + m*N) / R
	// if t < N
	// return t
	// else return t - N
	private void GetMontgomeryParms() {
		int NumberLength = this.NumberLength;
		int N, x, j;

		x = N = (int) TestNbr[0]; // 2 least significant bits of inverse
									// correct.
		x = x * (2 - N * x); // 4 least significant bits of inverse correct.
		x = x * (2 - N * x); // 8 least significant bits of inverse correct.
		x = x * (2 - N * x); // 16 least significant bits of inverse correct.
		x = x * (2 - N * x); // 32 least significant bits of inverse correct.
		MontgomeryMultN = (-x) & 0x7FFFFFFF;
		j = NumberLength;
		MontgomeryMultR1[j] = 1;
		do {
			MontgomeryMultR1[--j] = 0;
		} while (j > 0);
		AdjustModN(MontgomeryMultR1, TestNbr, NumberLength);
		MultBigNbrModN(MontgomeryMultR1, MontgomeryMultR1, MontgomeryMultR2, TestNbr, NumberLength);
		MontgomeryMult(MontgomeryMultR2, MontgomeryMultR2, MontgomeryMultAfterInv);
		AddBigNbrModN(MontgomeryMultR1, MontgomeryMultR1, MontgomeryMultR2, TestNbr, NumberLength);
	}

	private static void MultBigNbrModN(int Nbr1[], int Nbr2[], int Prod[], int TestNbr[], int NumberLength) {
		long MaxUInt = 0x7FFFFFFFL;
		int i, j;
		long Pr, Nbr;

		if (NumberLength >= 2 && TestNbr[NumberLength - 1] == 0 && TestNbr[NumberLength - 2] < 0x40000000) {
			NumberLength--;
		}
		i = NumberLength;
		do {
			Prod[--i] = 0;
		} while (i > 0);
		i = NumberLength;
		do {
			Nbr = Nbr1[--i];
			j = NumberLength;
			do {
				Prod[j] = Prod[j - 1];
				j--;
			} while (j > 0);
			Prod[0] = 0;
			Pr = 0;
			for (j = 0; j < NumberLength; j++) {
				Pr = (Pr >>> 31) + Nbr * Nbr2[j] + Prod[j];
				Prod[j] = (int) (Pr & MaxUInt);
			}
			Prod[j] += (Pr >>> 31);
			AdjustModN(Prod, TestNbr, NumberLength);
		} while (i > 0);
	}

	private static void MultBigNbrByLongModN(int Nbr1[], long Nbr2, int Prod[], int TestNbr[], int NumberLength) {
		long MaxUInt = 0x7FFFFFFFL;
		long Pr;
		int j;

		if (NumberLength >= 2 && TestNbr[NumberLength - 1] == 0 && TestNbr[NumberLength - 2] < 0x40000000) {
			NumberLength--;
		}
		Pr = 0;
		for (j = 0; j < NumberLength; j++) {
			Pr = (Pr >>> 31) + Nbr2 * Nbr1[j];
			Prod[j] = (int) (Pr & MaxUInt);
		}
		Prod[j] = (int) (Pr >>> 31);
		AdjustModN(Prod, TestNbr, NumberLength);
	}

	private static void AdjustModN(int Nbr[], int TestNbr[], int NumberLength) {
		long MaxUInt = 0x7FFFFFFFL;
		long TrialQuotient;
		long carry;
		int i;
		double dAux, dN;

		dN = (double) TestNbr[NumberLength - 1];
		if (NumberLength > 1) {
			dN += (double) TestNbr[NumberLength - 2] / dDosALa31;
		}
		if (NumberLength > 2) {
			dN += (double) TestNbr[NumberLength - 3] / dDosALa62;
		}
		dAux = (double) Nbr[NumberLength] * dDosALa31 + (double) Nbr[NumberLength - 1];
		if (NumberLength > 1) {
			dAux += (double) Nbr[NumberLength - 2] / dDosALa31;
		}
		TrialQuotient = (long) (dAux / dN) + 3;
		if (TrialQuotient >= DosALa32) {
			carry = 0;
			for (i = 0; i < NumberLength; i++) {
				carry = Nbr[i + 1] - (TrialQuotient >>> 31) * TestNbr[i] - carry;
				Nbr[i + 1] = (int) (carry & MaxUInt);
				carry = (MaxUInt - carry) >>> 31;
			}
			TrialQuotient &= MaxUInt;
		}
		carry = 0;
		for (i = 0; i < NumberLength; i++) {
			carry = Nbr[i] - TrialQuotient * TestNbr[i] - carry;
			Nbr[i] = (int) (carry & MaxUInt);
			carry = (MaxUInt - carry) >>> 31;
		}
		Nbr[NumberLength] -= (int) carry;
		while ((Nbr[NumberLength] & MaxUInt) != 0) {
			carry = 0;
			for (i = 0; i < NumberLength; i++) {
				carry += (long) Nbr[i] + (long) TestNbr[i];
				Nbr[i] = (int) (carry & MaxUInt);
				carry >>= 31;
			}
			Nbr[NumberLength] += (int) carry;
		}
	}

	private static void DivBigNbrByLong(int Dividend[], long Divisor, int Quotient[], int NumberLength) {
		int i;
		boolean ChSignDivisor = false;
		long Divid, Rem = 0;

		if (Divisor < 0) { // If divisor is negative...
			ChSignDivisor = true; // Indicate to change sign at the end and
			Divisor = -Divisor; // convert divisor to positive.
		}
		if (Dividend[i = NumberLength - 1] >= 0x40000000) { // If dividend is
															// negative...
			Rem = Divisor - 1;
		}
		for (; i >= 0; i--) {
			Divid = Dividend[i] + (Rem << 31);
			Rem = Divid - (Quotient[i] = (int) (Divid / Divisor)) * Divisor;
		}
		if (ChSignDivisor) { // Change sign if divisor is negative.
								// Convert divisor to positive.
			ChSignBigNbr(Quotient, NumberLength);
		}
	}

	private static long RemDivBigNbrByLong(int Dividend[], long Divisor, int NumberLength) {
		int i;
		long Rem = 0;
		long Mod2_31;
		int divis = (int) (Divisor < 0 ? -Divisor : Divisor);
		if (Divisor < 0) { // If divisor is negative...
			Divisor = -Divisor; // Convert divisor to positive.
		}
		Mod2_31 = ((-2147483648) - divis) % divis; // 2^31 mod divis.
		if (Dividend[i = NumberLength - 1] >= 0x40000000) { // If dividend is
															// negative...
			Rem = Divisor - 1;
		}
		for (; i >= 0; i--) {
			Rem = Rem * Mod2_31 + Dividend[i];
			do {
				Rem = (Rem >> 31) * Mod2_31 + (Rem & 0x7FFFFFFF);
			} while (Rem > 0x1FFFFFFFFL);
		}
		return Rem % divis;
	}

	// Gcd calculation:
	// Step 1: Set k<-0, and then repeatedly set k<-k+1, u<-u/2, v<-v/2
	// zero or more times until u and v are not both even.
	// Step 2: If u is odd, set t<-(-v) and go to step 4. Otherwise set t<-u.
	// Step 3: Set t<-t/2
	// Step 4: If t is even, go back to step 3.
	// Step 5: If t>0, set u<-t, otherwise set v<-(-t).
	// Step 6: Set t<-u-v. If t!=0, go back to step 3.
	// Step 7: The GCD is u*2^k.
	private void GcdBigNbr(int Nbr1[], int Nbr2[], int Gcd[], int NumberLength) {
		int i, k;

		System.arraycopy(Nbr1, 0, CalcAuxGcdU, 0, NumberLength);
		System.arraycopy(Nbr2, 0, CalcAuxGcdV, 0, NumberLength);
		for (i = 0; i < NumberLength; i++) {
			if (CalcAuxGcdU[i] != 0) {
				break;
			}
		}
		if (i == NumberLength) {
			System.arraycopy(CalcAuxGcdV, 0, Gcd, 0, NumberLength);
			return;
		}
		for (i = 0; i < NumberLength; i++) {
			if (CalcAuxGcdV[i] != 0) {
				break;
			}
		}
		if (i == NumberLength) {
			System.arraycopy(CalcAuxGcdU, 0, Gcd, 0, NumberLength);
			return;
		}
		if (CalcAuxGcdU[NumberLength - 1] >= 0x40000000L) {
			ChSignBigNbr(CalcAuxGcdU, NumberLength);
		}
		if (CalcAuxGcdV[NumberLength - 1] >= 0x40000000L) {
			ChSignBigNbr(CalcAuxGcdV, NumberLength);
		}
		k = 0;
		while ((CalcAuxGcdU[0] & 1) == 0 && (CalcAuxGcdV[0] & 1) == 0) { // Step
																			// 1
			k++;
			DivBigNbrByLong(CalcAuxGcdU, 2, CalcAuxGcdU, NumberLength);
			DivBigNbrByLong(CalcAuxGcdV, 2, CalcAuxGcdV, NumberLength);
		}
		if ((CalcAuxGcdU[0] & 1) == 1) { // Step 2
			System.arraycopy(CalcAuxGcdV, 0, CalcAuxGcdT, 0, NumberLength);
			ChSignBigNbr(CalcAuxGcdT, NumberLength);
		} else {
			System.arraycopy(CalcAuxGcdU, 0, CalcAuxGcdT, 0, NumberLength);
		}
		do {
			while ((CalcAuxGcdT[0] & 1) == 0) { // Step 4
				DivBigNbrByLong(CalcAuxGcdT, 2, CalcAuxGcdT, NumberLength); // Step
																			// 3
			}
			if (CalcAuxGcdT[NumberLength - 1] < 0x40000000L) { // Step 5
				System.arraycopy(CalcAuxGcdT, 0, CalcAuxGcdU, 0, NumberLength);
			} else {
				System.arraycopy(CalcAuxGcdT, 0, CalcAuxGcdV, 0, NumberLength);
				ChSignBigNbr(CalcAuxGcdV, NumberLength);
			} // Step 6
			SubtractBigNbr(CalcAuxGcdU, CalcAuxGcdV, CalcAuxGcdT, NumberLength);
			for (i = 0; i < NumberLength; i++) {
				if (CalcAuxGcdT[i] != 0) {
					break;
				}
			}
		} while (i != NumberLength);
		System.arraycopy(CalcAuxGcdU, 0, Gcd, 0, NumberLength); // Step 7
		while (k > 0) {
			AddBigNbr(Gcd, Gcd, Gcd, NumberLength);
			k--;
		}
	}

	private static void Convert31To32Bits(int[] nbr31bits, long[] nbr32bits, int NumberLength) {
		int i, j, k;
		i = 0;
		for (j = -1; j < NumberLength; j++) {
			k = i % 31;
			if (k == 0) {
				j++;
			}
			if (j == NumberLength) {
				break;
			}
			if (j == NumberLength - 1) {
				nbr32bits[i] = nbr31bits[j] >> k;
			} else {
				nbr32bits[i] = ((nbr31bits[j] >> k) | (nbr31bits[j + 1] << (31 - k))) & 0xFFFFFFFFL;
			}
			i++;
		}
		for (; i < NumberLength; i++) {
			nbr32bits[i] = 0;
		}
	}

	private static void Convert32To31Bits(long[] nbr32bits, int[] nbr31bits, int NumberLength) {
		int i, j, k;
		j = 0;
		nbr32bits[NumberLength] = 0;
		for (i = 0; i < NumberLength; i++) {
			k = i & 0x0000001F;
			if (k == 0) {
				nbr31bits[i] = (int) (nbr32bits[j] & 0x7FFFFFFF);
			} else {
				nbr31bits[i] = (int) (((nbr32bits[j] >> (32 - k)) | (nbr32bits[j + 1] << k)) & 0x7FFFFFFF);
				j++;
			}
		}
	}

	/***********************************************************************/
	/* NAME: ModInvBigNbr */
	/*                                                                     */
	/* PURPOSE: Find the inverse multiplicative modulo v. */
	/*                                                                     */
	/* The algorithm terminates with u1 = u^(-1) MOD v. */
	/***********************************************************************/
	private void ModInvBigNbr(int[] a, int[] inv, int[] b, int NumberLength) {
		int i;
		int Dif, E;
		int st1, st2;
		long Yaa, Yab; // 2^E * A' = Yaa A + Yab B
		long Yba, Ybb; // 2^E * B' = Yba A + Ybb B
		long Ygb0; // 2^E * Mu' = Yaa Mu + Yab Gamma + Ymb0 B0
		long Ymb0; // 2^E * Gamma' = Yba Mu + Ybb Gamma + Ygb0 B0
		int Iaa, Iab, Iba, Ibb;
		long Tmp1, Tmp2, Tmp3, Tmp4, Tmp5;
		int B0l;
		int invB0l;
		int Al, Bl, T1, Gl, Ml;
		long carry1, carry2, carry3, carry4;
		int Yaah, Yabh, Ybah, Ybbh;
		int Ymb0h, Ygb0h;
		long Pr1, Pr2, Pr3, Pr4, Pr5, Pr6, Pr7;
		long[] B = this.CalcAuxModInvBB;
		long[] CalcAuxModInvA = this.CalcAuxModInvA;
		long[] CalcAuxModInvB = this.CalcAuxModInvB;
		long[] CalcAuxModInvMu = this.CalcAuxModInvMu;
		long[] CalcAuxModInvGamma = this.CalcAuxModInvGamma;

		if (NumberLength >= 2 && b[NumberLength - 1] == 0 && b[NumberLength - 2] < 0x40000000) {
			NumberLength--;
		}
		Convert31To32Bits(a, CalcAuxModInvA, NumberLength);
		Convert31To32Bits(b, CalcAuxModInvB, NumberLength);
		System.arraycopy(CalcAuxModInvB, 0, B, 0, NumberLength);
		B0l = (int) B[0];
		invB0l = B0l; // 2 least significant bits of inverse correct.
		invB0l = invB0l * (2 - B0l * invB0l); // 4 LSB of inverse correct.
		invB0l = invB0l * (2 - B0l * invB0l); // 8 LSB of inverse correct.
		invB0l = invB0l * (2 - B0l * invB0l); // 16 LSB of inverse correct.
		invB0l = invB0l * (2 - B0l * invB0l); // 32 LSB of inverse correct.
		for (i = NumberLength - 1; i >= 0; i--) {
			CalcAuxModInvGamma[i] = 0;
			CalcAuxModInvMu[i] = 0;
		}
		CalcAuxModInvMu[0] = 1;
		Dif = 0;
		outer_loop: for (;;) {
			Iaa = Ibb = 1;
			Iab = Iba = 0;
			Al = (int) CalcAuxModInvA[0];
			Bl = (int) CalcAuxModInvB[0];
			E = 0;
			if (Bl == 0) {
				for (i = NumberLength - 1; i >= 0; i--) {
					if (CalcAuxModInvB[i] != 0)
						break;
				}
				if (i < 0)
					break; // Go out of loop if CalcAuxModInvB = 0
			}
			for (;;) {
				T1 = 0;
				while ((Bl & 1) == 0) {
					if (E == 31) {
						Yaa = Iaa;
						Yab = Iab;
						Yba = Iba;
						Ybb = Ibb;
						Gl = (int) CalcAuxModInvGamma[0];
						Ml = (int) CalcAuxModInvMu[0];
						Dif++;
						T1++;
						Yaa <<= T1;
						Yab <<= T1;
						Ymb0 = (-(int) Yaa * Ml - (int) Yab * Gl) * invB0l;
						Ygb0 = (-Iba * Ml - Ibb * Gl) * invB0l;
						carry1 = carry2 = carry3 = carry4 = 0;
						Yaah = (int) (Yaa >> 32);
						Yabh = (int) (Yab >> 32);
						Ybah = (int) (Yba >> 32);
						Ybbh = (int) (Ybb >> 32);
						Ymb0h = (int) (Ymb0 >> 32);
						Ygb0h = (int) (Ygb0 >> 32);
						Yaa &= 0xFFFFFFFFL;
						Yab &= 0xFFFFFFFFL;
						Yba &= 0xFFFFFFFFL;
						Ybb &= 0xFFFFFFFFL;
						Ymb0 &= 0xFFFFFFFFL;
						Ygb0 &= 0xFFFFFFFFL;

						st1 = Yaah * 6 + Yabh * 2 + Ymb0h;
						st2 = Ybah * 6 + Ybbh * 2 + Ygb0h;
						for (i = 0; i < NumberLength; i++) {
							Pr1 = Yaa * (Tmp1 = CalcAuxModInvMu[i]);
							Pr2 = Yab * (Tmp2 = CalcAuxModInvGamma[i]);
							Pr3 = Ymb0 * (Tmp3 = B[i]);
							Pr4 = (Pr1 & 0xFFFFFFFFL) + (Pr2 & 0xFFFFFFFFL) + (Pr3 & 0xFFFFFFFFL) + carry3;
							Pr5 = Yaa * (Tmp4 = CalcAuxModInvA[i]);
							Pr6 = Yab * (Tmp5 = CalcAuxModInvB[i]);
							Pr7 = (Pr5 & 0xFFFFFFFFL) + (Pr6 & 0xFFFFFFFFL) + carry1;
							switch (st1) {
							case -9:
								carry3 = -Tmp1 - Tmp2 - Tmp3;
								carry1 = -Tmp4 - Tmp5;
								break;
							case -8:
								carry3 = -Tmp1 - Tmp2;
								carry1 = -Tmp4 - Tmp5;
								break;
							case -7:
								carry3 = -Tmp1 - Tmp3;
								carry1 = -Tmp4;
								break;
							case -6:
								carry3 = -Tmp1;
								carry1 = -Tmp4;
								break;
							case -5:
								carry3 = -Tmp1 + Tmp2 - Tmp3;
								carry1 = -Tmp4 + Tmp5;
								break;
							case -4:
								carry3 = -Tmp1 + Tmp2;
								carry1 = -Tmp4 + Tmp5;
								break;
							case -3:
								carry3 = -Tmp2 - Tmp3;
								carry1 = -Tmp5;
								break;
							case -2:
								carry3 = -Tmp2;
								carry1 = -Tmp5;
								break;
							case -1:
								carry3 = -Tmp3;
								carry1 = 0;
								break;
							case 0:
								carry3 = 0;
								carry1 = 0;
								break;
							case 1:
								carry3 = Tmp2 - Tmp3;
								carry1 = Tmp5;
								break;
							case 2:
								carry3 = Tmp2;
								carry1 = Tmp5;
								break;
							case 3:
								carry3 = Tmp1 - Tmp2 - Tmp3;
								carry1 = Tmp4 - Tmp5;
								break;
							case 4:
								carry3 = Tmp1 - Tmp2;
								carry1 = Tmp4 - Tmp5;
								break;
							case 5:
								carry3 = Tmp1 - Tmp3;
								carry1 = Tmp4;
								break;
							case 6:
								carry3 = Tmp1;
								carry1 = Tmp4;
								break;
							case 7:
								carry3 = Tmp1 + Tmp2 - Tmp3;
								carry1 = Tmp4 + Tmp5;
								break;
							case 8:
								carry3 = Tmp1 + Tmp2;
								carry1 = Tmp4 + Tmp5;
								break;
							}
							carry3 += (Pr1 >>> 32) + (Pr2 >>> 32) + (Pr3 >>> 32) + (Pr4 >> 32);
							carry1 += (Pr5 >>> 32) + (Pr6 >>> 32) + (Pr7 >> 32);
							if (i > 0) {
								CalcAuxModInvMu[i - 1] = Pr4 & 0xFFFFFFFFL;
								CalcAuxModInvA[i - 1] = Pr7 & 0xFFFFFFFFL;
							}
							Pr1 = Yba * Tmp1;
							Pr2 = Ybb * Tmp2;
							Pr3 = Ygb0 * Tmp3;
							Pr4 = (Pr1 & 0xFFFFFFFFL) + (Pr2 & 0xFFFFFFFFL) + (Pr3 & 0xFFFFFFFFL) + carry4;
							Pr5 = Yba * Tmp4;
							Pr6 = Ybb * Tmp5;
							Pr7 = (Pr5 & 0xFFFFFFFFL) + (Pr6 & 0xFFFFFFFFL) + carry2;
							switch (st2) {
							case -9:
								carry4 = -Tmp1 - Tmp2 - Tmp3;
								carry2 = -Tmp4 - Tmp5;
								break;
							case -8:
								carry4 = -Tmp1 - Tmp2;
								carry2 = -Tmp4 - Tmp5;
								break;
							case -7:
								carry4 = -Tmp1 - Tmp3;
								carry2 = -Tmp4;
								break;
							case -6:
								carry4 = -Tmp1;
								carry2 = -Tmp4;
								break;
							case -5:
								carry4 = -Tmp1 + Tmp2 - Tmp3;
								carry2 = -Tmp4 + Tmp5;
								break;
							case -4:
								carry4 = -Tmp1 + Tmp2;
								carry2 = -Tmp4 + Tmp5;
								break;
							case -3:
								carry4 = -Tmp2 - Tmp3;
								carry2 = -Tmp5;
								break;
							case -2:
								carry4 = -Tmp2;
								carry2 = -Tmp5;
								break;
							case -1:
								carry4 = -Tmp3;
								carry2 = 0;
								break;
							case 0:
								carry4 = 0;
								carry2 = 0;
								break;
							case 1:
								carry4 = Tmp2 - Tmp3;
								carry2 = Tmp5;
								break;
							case 2:
								carry4 = Tmp2;
								carry2 = Tmp5;
								break;
							case 3:
								carry4 = Tmp1 - Tmp2 - Tmp3;
								carry2 = Tmp4 - Tmp5;
								break;
							case 4:
								carry4 = Tmp1 - Tmp2;
								carry2 = Tmp4 - Tmp5;
								break;
							case 5:
								carry4 = Tmp1 - Tmp3;
								carry2 = Tmp4;
								break;
							case 6:
								carry4 = Tmp1;
								carry2 = Tmp4;
								break;
							case 7:
								carry4 = Tmp1 + Tmp2 - Tmp3;
								carry2 = Tmp4 + Tmp5;
								break;
							case 8:
								carry4 = Tmp1 + Tmp2;
								carry2 = Tmp4 + Tmp5;
								break;
							}
							carry4 += (Pr1 >>> 32) + (Pr2 >>> 32) + (Pr3 >>> 32) + (Pr4 >> 32);
							carry2 += (Pr5 >>> 32) + (Pr6 >>> 32) + (Pr7 >> 32);
							if (i > 0) {
								CalcAuxModInvGamma[i - 1] = Pr4 & 0xFFFFFFFFL;
								CalcAuxModInvB[i - 1] = Pr7 & 0xFFFFFFFFL;
							}
						}

						if ((int) CalcAuxModInvA[i - 1] < 0) {
							carry1 -= Yaa;
							carry2 -= Yba;
						}
						if ((int) CalcAuxModInvB[i - 1] < 0) {
							carry1 -= Yab;
							carry2 -= Ybb;
						}
						if ((int) CalcAuxModInvMu[i - 1] < 0) {
							carry3 -= Yaa;
							carry4 -= Yba;
						}
						if ((int) CalcAuxModInvGamma[i - 1] < 0) {
							carry3 -= Yab;
							carry4 -= Ybb;
						}
						CalcAuxModInvA[i - 1] = carry1 & 0xFFFFFFFFL;
						CalcAuxModInvB[i - 1] = carry2 & 0xFFFFFFFFL;
						CalcAuxModInvMu[i - 1] = carry3 & 0xFFFFFFFFL;
						CalcAuxModInvGamma[i - 1] = carry4 & 0xFFFFFFFFL;
						continue outer_loop;
					}
					Bl >>= 1;
					Dif++;
					E++;
					T1++;
				} /* end while */
				Iaa <<= T1;
				Iab <<= T1;
				if (Dif >= 0) {
					Dif = -Dif;
					if (((Al + Bl) & 3) == 0) {
						T1 = Iba;
						Iba += Iaa;
						Iaa = T1;
						T1 = Ibb;
						Ibb += Iab;
						Iab = T1;
						T1 = Bl;
						Bl += Al;
						Al = T1;
					} else {
						T1 = Iba;
						Iba -= Iaa;
						Iaa = T1;
						T1 = Ibb;
						Ibb -= Iab;
						Iab = T1;
						T1 = Bl;
						Bl -= Al;
						Al = T1;
					}
				} else {
					if (((Al + Bl) & 3) == 0) {
						Iba += Iaa;
						Ibb += Iab;
						Bl += Al;
					} else {
						Iba -= Iaa;
						Ibb -= Iab;
						Bl -= Al;
					}
				}
				Dif--;
			}
		}
		if (CalcAuxModInvA[0] != 1) {
			SubtractBigNbr32(B, CalcAuxModInvMu, CalcAuxModInvMu, NumberLength);
		}
		if ((int) CalcAuxModInvMu[i = NumberLength - 1] < 0) {
			AddBigNbr32(B, CalcAuxModInvMu, CalcAuxModInvMu, NumberLength);
		}
		for (; i >= 0; i--) {
			if (B[i] != CalcAuxModInvMu[i])
				break;
		}
		if (i < 0 || B[i] < CalcAuxModInvMu[i]) { // If B < Mu
			SubtractBigNbr32(CalcAuxModInvMu, B, CalcAuxModInvMu, NumberLength); // Mu <- Mu - B
		}
		Convert32To31Bits(CalcAuxModInvMu, inv, NumberLength);
	}

}
