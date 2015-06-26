package com.stevebrecher;

/**
 * Non-instantiable class containing a variety of static poker hand evaluation and related utility methods.
 * <p>
 * All of the methods are thread-safe.
 * <p>
 * Each evaluation method takes a single parameter representing a hand of five to
 * seven cards represented within a long (64 bits).  The long is considered as
 * composed of four 16-bit fields, one for each suit.  The ordering of these
 * 16-bit fields within the long, i.e., the correspondence of each to a specific
 * suit, is immaterial.  Within each suit's 16-bit field, the least-significant
 * 13 bits (masked by 0x1FFF) are flags representing the presence of ranks in
 * that suit, where bit 0 set (0x0001) for a deuce, ..., bit 12 set (0x1000) for
 * an ace.  The values of the unused most-significant three bits within each
 * 16-bit suit field are immaterial.
 * <p>
 * A hand parameter can be built by encoding a {@link CardSet} or by bitwise
 * OR-ing, or adding, the encoded values of individual {@link Card}s.  These
 * encodings are returned by an {@link #encode encode} method.
 * <p>
 * Different methods are used for high and for lowball evaluation.
 * <p>
 * For high evaluation if results R1 > R2, hand 1 beats hand 2;
 * for lowball evaluation if results R1 > R2, hand 2 beats hand 1.
 * <p>
 * Evaluation result in 32 bits = 0x0V0RRRRR where V, R are
 * hex digits or "nybbles" (half-bytes).
 * <p>
 * V nybble = category code ranging from {@link HandCategory#NO_PAIR}<code>.ordinal()</code>
 *                                    to {@link HandCategory#STRAIGHT_FLUSH}<code>.ordinal()</code>
 * <p>
 * The R nybbles are the significant ranks (0..12), where 0 is the deuce
 * in a high result (Ace is 12, 0xC), and for lowball 0 is the Ace
 * (King is 0xC).  The Rs may be considered to consist of Ps for ranks
 * which determine the primary value of the hand, and Ks for kickers
 * where applicable.  Ordering is left-to-right:  first the Ps, then
 * any Ks, then padding with 0s.  Because 0 is a valid rank, to
 * interpret a result you must know how many ranks are significant,
 * which is a function of the hand category and whether high or lowball.
 * Examples: for a one-pair hand there are four significant ranks,
 * that of the pair and of the three kickers; for a straight, there is
 * one significant rank, that of the highest in the hand.
 * <p>
 * Common-card (board) games are assumed in determining the number of
 * significant ranks.  For example, a kicker value is returned for quads even
 * though it wouldn't be significant in a draw game.
 * <p><pre>
 * Examples of ...Eval method results (high where not indicated):
 *  Royal flush: 0x080C0000
 *  Four of a kind, Queens, with a 5 kicker:  0x070A3000
 *  Threes full of eights:  0x06016000
 *  Straight to the five (wheel): 0x04030000 (high)
 *  Straight to the five (wheel): 0x04040000 (lowball)
 *  One pair, deuces (0x0), with A65: 0x0100C430 (high)
 *  One pair, deuces (0x1), with 65A: 0x01015400 (lowball)
 *  No pair, KJT85: 0x000B9863
 *  Razz, wheel:  0x00043210</pre>
 * For the eight-or-better lowball ..._Eval functions, the result is
 * either as above or the constant {@link #NO_8_LOW}.  NO_8_LOW > any other
 * ..._Eval function result.
 * <p>
 * @version 2010Jun25.1
 * @author Steve Brecher
 *
 */
// 2010Jun25.1
//		Fix handRazzEval return for full house for AAAABBB rank pattern (make the
//			lower rank the trips rank and the other the pair rank rather than vice-versa).
// 2010Jun25.0
//		Fix hand7Eval return value for quads+trips (case 2 of its switch).
//		Fix kicker ordering for handRazzEval one pair for AABBCCD rank pattern.
// 2010Jun24.0
//		Fix one pair value returned by handAto5LoEval.
// 2010Jun23.1
//		In Omaha8LowEval add short circuits to avoid evaluation if board has fewer than 3 8OB cards
//			and for pairs of hole cards which are not both 8OB.
// 2010Jun23.0
//		Fix Omaha8LowEval to use previous algorithm, but separately on each pair of hole cards.
// 2010Jun22.2
//		Fix coding error in Omaha8LowEval that would return incorrect result when
//			third and fourth hole cards make the best 8OB low.
// 2010Jun22.1
//		Omaha8LowEval rewritten with different interface.  Previous algorithmic trick
//			to avoid brute force of six (4C2) computations didn't work.
//		lo3_8OBMask array omitted -- no longer used.
// 2010Jun22.0
//		Fix initialization of lo3_8OBMask and loMaskOrNo8Low.
// 2010Jun21.4
//		Change name of method from hand5Ato5LoEval to handAto5LoEval.
// 2010Jun21.3
//		Fix return value of hand8LowEval.
//		Fix bit-wise rotation of parameter fields in low eval methods: "^"s to "&"s.
// 2010Jun21.2
//		Fix initialization of straightValue[0x100F], i.e., wheel.
//		Fix return values for A5432 and A5432 flush in hand2to7LoEval.
// 2010Jun21.1
//		Fix loop initialization in initializer block to fix eval result rank values.
// 2010Jun21.0
//		Fix deconstruction of most eval method parameters into suit fields.
// 2010Jun20.1
//		Fix return value of hand2to7LoEval for an A5432 flush.  This method was
//		introduced in 2010.Jun20.0.
// 2010Jun20.0
//		Conform method parameters and results to C version: CHANGED API and fewer arrays.
// 2010Jun19.0
//		fix rotations of ace bits from bit 13 to bit 0 in
//		handRazzEval, hand5Ato5LoEval, ranksMaskLo, hand8LowEval
// 2008Apr27.0
//		fix hand6Eval's calls to flushAndOrStraight6
// 2006Dec05.0
//		original Java release, ported from C

public final class HandEval {

	private HandEval() {}	// no instances
	
	/**
	 * Returns a value which can be used in building a parameter to one of the HandEval evaluation methods.
	 * @param card a {@link Card}
	 * @return a value which may be bitwise OR'ed or added to other such
	 * values to build a parameter to one of the HandEval evaluation methods.
	 */
	/*
	public static long encode(final Card card) {
		return 0x1L << (card.suitOf().ordinal()*16 + card.rankOf().ordinal());
	}
	 */
	/**
	 * Returns a value which can be used as a parameter to one of the HandEval evaluation methods.
	 * @param cs a {@link CardSet}
	 * @return a value which can be used as a parameter to one of the HandEval evaluation methods.
	 * The value may also be bitwise OR'ed or added to other such
	 * values to build an evaluation method parameter.
	 */
	/*
	public static long encode(final CardSet cs) {
		long result = 0;
		for (Card c : cs)
			result |= encode(c);
		return result;
	}
	*/
	public static enum HandCategory { NO_PAIR, PAIR, TWO_PAIR, THREE_OF_A_KIND, STRAIGHT,
							FLUSH, FULL_HOUSE, FOUR_OF_A_KIND, STRAIGHT_FLUSH; }

	private static final int   RANK_SHIFT_1		= 4;
	private static final int   RANK_SHIFT_2		= RANK_SHIFT_1 + 4;
	private static final int   RANK_SHIFT_3		= RANK_SHIFT_2 + 4;
	private static final int   RANK_SHIFT_4		= RANK_SHIFT_3 + 4;
	public static final int    VALUE_SHIFT		= RANK_SHIFT_4 + 8;

	private static final int   NO_PAIR			= 0;
	private static final int   PAIR				= NO_PAIR			+ (1 << VALUE_SHIFT);
	private static final int   TWO_PAIR			= PAIR				+ (1 << VALUE_SHIFT);
	private static final int   THREE_OF_A_KIND	= TWO_PAIR			+ (1 << VALUE_SHIFT);
	private static final int   STRAIGHT			= THREE_OF_A_KIND	+ (1 << VALUE_SHIFT);
	private static final int   FLUSH			= STRAIGHT			+ (1 << VALUE_SHIFT);
	private static final int   FULL_HOUSE		= FLUSH				+ (1 << VALUE_SHIFT);
	private static final int   FOUR_OF_A_KIND	= FULL_HOUSE		+ (1 << VALUE_SHIFT);
	private static final int   STRAIGHT_FLUSH	= FOUR_OF_A_KIND	+ (1 << VALUE_SHIFT);

	/**
	 *  Greater than any return value of the HandEval evaluation methods.
	 */
	public static final int NO_8_LOW = STRAIGHT_FLUSH + (1 << VALUE_SHIFT);

	private static final int   ARRAY_SIZE		= 0x1FC0 + 1;			// all combos of up to 7 of LS 13 bits on
	/* Arrays for which index is bit mask of card ranks in hand: */
	private static final int[] straightValue	= new int[ARRAY_SIZE];	// Value(STRAIGHT) | (straight's high card rank-2 (3..12) << RANK_SHIFT_4); 0 if no straight
	private static final int[] nbrOfRanks		= new int[ARRAY_SIZE];	// count of bits set
	private static final int[] hiRank			= new int[ARRAY_SIZE];	// 4-bit card rank of highest bit set, right justified
	private static final int[] hiUpTo5Ranks		= new int[ARRAY_SIZE];	// 4-bit card ranks of highest (up to) 5 bits set, right-justified
	private static final int[] loMaskOrNo8Low	= new int[ARRAY_SIZE];	// low-order 5 of the low-order 8 bits set, or NO_8_LOW; Ace is LS bit.
	private static final int[] lo3_8OBRanksMask	= new int[ARRAY_SIZE];	// bits other than lowest 3 8-or-better reset; Ace is LS bit.

	private static int flushAndOrStraight7(final int ranks, final int c, final int d, final int h, final int s) {

		int	i, j;
		
		if ((j = nbrOfRanks[c]) > 7 - 5) {
			// there's either a club flush or no flush
			if (j >= 5)
				if ((i = straightValue[c]) == 0)
					return FLUSH | hiUpTo5Ranks[c];
				else
					return (STRAIGHT_FLUSH - STRAIGHT) + i;
		} else if ((j += (i = nbrOfRanks[d])) > 7 - 5) {
			if (i >= 5)
				if ((i = straightValue[d]) == 0)
					return FLUSH | hiUpTo5Ranks[d];
				else
					return (STRAIGHT_FLUSH - STRAIGHT) + i;
		} else if ((j += (i = nbrOfRanks[h])) > 7 - 5) {
			if (i >= 5)
				if ((i = straightValue[h]) == 0)
					return FLUSH | hiUpTo5Ranks[h];
				else
					return (STRAIGHT_FLUSH - STRAIGHT) + i;
		} else
			/* total cards in other suits <= 7-5: spade flush: */
			if ((i = straightValue[s]) == 0)
				return FLUSH | hiUpTo5Ranks[s];
			else
				return (STRAIGHT_FLUSH - STRAIGHT) + i;
		return straightValue[ranks];
	}	
	
	/**
	 * Returns the value of the best 5-card high poker hand from 7 cards.
	 * @param hand bit mask with one bit set for each of 7 cards.
	 * @return the value of the best 5-card high poker hand.
	 */
	public static int hand7Eval(long hand) {
		int i, j, ranks;

		/* 
		 * The parameter contains four 16-bit fields; in each, the low-order
		 * 13 bits are significant.  Get the respective fields into variables.
		 * We don't care which suit is which; we arbitrarily call them c,d,h,s.
		 */
		final int c = (int)hand & 0x1FFF;
		final int d = ((int)hand >>> 16) & 0x1FFF;
		final int h = (int)(hand >>> 32) & 0x1FFF;
		final int s = (int)(hand >>> 48) & 0x1FFF;

		switch (nbrOfRanks[ranks = c | d | h | s]) {

		case 2:
		/*
		 * quads with trips kicker
		 */
			i = c & d & h & s; /* bit for quads */
			return FOUR_OF_A_KIND | (hiRank[i] << RANK_SHIFT_4) | (hiRank[i ^ ranks] << RANK_SHIFT_3);

		case 3:
		/*
		 * trips and pair (full house) with non-playing pair,
		 * or two trips (full house) with non-playing singleton,
		 * or quads with pair and singleton
		 */
			/* bits for singleton, if any, and trips, if any: */
			if (nbrOfRanks[i = c ^ d ^ h ^ s] == 3) {
				/* two trips (full house) with non-playing singleton */
				if (nbrOfRanks[i = c & d] != 2)
					if (nbrOfRanks[i = c & h] != 2)
						if (nbrOfRanks[i = c & s] != 2)
							if (nbrOfRanks[i = d & h] != 2)
								if (nbrOfRanks[i = d & s] != 2)
									i = h & s; /* bits for the trips */
				return FULL_HOUSE | (hiUpTo5Ranks[i] << RANK_SHIFT_3);
			}
			if ((j = c & d & h & s) != 0) /* bit for quads */
				/* quads with pair and singleton */
				return FOUR_OF_A_KIND | (hiRank[j] << RANK_SHIFT_4) | (hiRank[ranks ^ j] << RANK_SHIFT_3);
			/* trips and pair (full house) with non-playing pair */
			return FULL_HOUSE | (hiRank[i] << RANK_SHIFT_4) | (hiRank[ranks ^ i] << RANK_SHIFT_3);

		case 4:
		/*
		 * three pair and singleton,
		 * or trips and pair (full house) and two non-playing singletons,
		 * or quads with singleton kicker and two non-playing singletons
		 */
			i = c ^ d ^ h ^ s; // the bit(s) of the trips, if any, and singleton(s)
			if (nbrOfRanks[i] == 1) {
				/* three pair and singleton */
				j = hiUpTo5Ranks[ranks ^ i];	/* ranks of the three pairs */
				return TWO_PAIR | ((j & 0x0FF0) << RANK_SHIFT_2) | (hiRank[i | (1 << (j & 0x000F))] << RANK_SHIFT_2);
			}
			if ((j = c & d & h & s) == 0) {
				// trips and pair (full house) and two non-playing singletons
				i ^= ranks; /* bit for the pair */
				if ((j = (c & d) & (~i)) == 0)
					j = (h & s) & (~i); /* bit for the trips */
				return FULL_HOUSE | (hiRank[j] << RANK_SHIFT_4) | (hiRank[i] << RANK_SHIFT_3);
			}
			// quads with singleton kicker and two non-playing singletons
			return FOUR_OF_A_KIND | (hiRank[j] << RANK_SHIFT_4) | (hiRank[i] << RANK_SHIFT_3);

		case 5:
		/*
		 * flush and/or straight,
		 * or two pair and three singletons,
		 * or trips and four singletons
		 */
			if ((i = flushAndOrStraight7(ranks, c, d, h, s)) != 0)
				return i;
			i = c ^ d ^ h ^ s; // the bits of the trips, if any, and singletons
			if (nbrOfRanks[i] != 5)
				/* two pair and three singletons */
				return TWO_PAIR | (hiUpTo5Ranks[i ^ ranks] << RANK_SHIFT_3) | (hiRank[i] << RANK_SHIFT_2);
			/* trips and four singletons */
			if ((j = c & d) == 0)
				j = h & s;
			// j has trips bit
			return THREE_OF_A_KIND | (hiRank[j] << RANK_SHIFT_4) | (hiUpTo5Ranks[i ^ j] & 0x0FF00);

		case 6:
		/*
		 * flush and/or straight,
		 * or one pair and three kickers and two nonplaying singletons
		 */
			if ((i = flushAndOrStraight7(ranks, c, d, h, s)) != 0)
				return i;
			i = c ^ d ^ h ^ s; /* the bits of the five singletons */
			return PAIR | (hiRank[ranks ^ i] << RANK_SHIFT_4) | ((hiUpTo5Ranks[i] & 0x0FFF00) >> RANK_SHIFT_1);

		case 7:
		/*
		 * flush and/or straight or no pair
		 */
			if ((i = flushAndOrStraight7(ranks, c, d, h, s)) != 0)
				return i;
			return  NO_PAIR | hiUpTo5Ranks[ranks];

		} /* end switch */

		return 0; /* never reached, but avoids compiler warning */
	}


	/**
	 * Returns the value of the best 5-card Razz poker hand from 7 cards.
	 * @param hand bit mask with one bit set for each of 7 cards.
	 * @return the value of the best 5-card Razz poker hand.
	 */
	public static int handRazzEval(long hand) {

		// each of the following extracts a 13-bit field from hand and
		// rotates it left to position the ace in the least significant bit
		final int c = (((int)hand & 0x0FFF) << 1)  + (((int)hand & 0x1000) >> 12);
		final int d = (((int)hand >> 15) & 0x1FFE) + (((int)hand & (0x1000 << 16)) >> 28);
		final int h = ((int)(hand >> 31) & 0x1FFE) + (int)((hand & (0x1000L << 32)) >> 44);
		final int s = ((int)(hand >> 47) & 0x1FFE) + (int)((hand & (0x1000L << 48)) >> 60);

		final int ranks = c | d | h | s;
		int i, j;

		switch (nbrOfRanks[ranks]) {

		case 2:
			/* AAAABBB -- full house */
			i = c & d & h & s; /* bit for quads */
			j = i ^ ranks; /* bit for trips */
			// it can't matter in comparison of results from a 52-card deck,
			// but we return the correct value per relative ranks
			if (i < j)
				return FULL_HOUSE | (hiRank[i] << RANK_SHIFT_4) | (hiRank[j] << RANK_SHIFT_3);
			return FULL_HOUSE | (hiRank[j] << RANK_SHIFT_4) | (hiRank[i] << RANK_SHIFT_3);

		case 3:
			/*
			 * AAABBBC -- two pair,
			 * AAAABBC -- two pair,
			 * AAABBCC -- two pair w/ kicker = highest rank.
			 */
			/* bits for singleton, if any, and trips, if any: */
			if (nbrOfRanks[i = c ^ d ^ h ^ s] == 3) {
				/* odd number of each rank: AAABBBC -- two pair */
				if (nbrOfRanks[i = c & d] != 2)
					if (nbrOfRanks[i = c & h] != 2)
						if (nbrOfRanks[i = c & s] != 2)
							if (nbrOfRanks[i = d & h] != 2)
								if (nbrOfRanks[i = d & s] != 2)
									i = h & s; /* bits for the trips */
				return TWO_PAIR | (hiUpTo5Ranks[i] << RANK_SHIFT_3) | (hiRank[ranks ^ i] << RANK_SHIFT_2);
			}
			if ((j = c & d & h & s) != 0)  /* bit for quads */
				/* AAAABBC -- two pair */
				return TWO_PAIR | (hiUpTo5Ranks[ranks ^ i] << RANK_SHIFT_3) | (hiRank[i] << RANK_SHIFT_2);
			/* AAABBCC -- two pair w/ kicker = highest rank */
			i = hiUpTo5Ranks[ranks]; /* 00KPP */
			return TWO_PAIR | ((i | (i << RANK_SHIFT_3)) & 0x0FFF00);	// TWO_PAIR | (KPPKPP & 0x0FFF00)

		case 4:
			/*
			 * AABBCCD -- one pair (lowest of A, B, C),
			 * AAABBCD -- one pair (A or B),
			 * AAAABCD -- one pair (A)
			 */
			i = c ^ d ^ h ^ s; /* the bit(s) of the trips, if any,
			 and singleton(s) */
			if (nbrOfRanks[i] == 1) {
				/* AABBCCD -- one pair, C with ABD; D's bit is in i */
				j = ranks ^ i;	// ABC bits
				int k = hiUpTo5Ranks[j] & 0x0000F;	// C rank
				i |= j ^ (1 << (k));	// ABD bits
				return PAIR | (k << RANK_SHIFT_4) | (hiUpTo5Ranks[i] << RANK_SHIFT_1);
			}
			if ((j = c & d & h & s) == 0) {
				/* AAABBCD -- one pair (A or B) */
				i ^= ranks; /* bit for B */
				if ((j = (c & d) & (~i)) == 0)
					j = (h & s) & (~i); /* bit for A */
				if (i < j)
					return PAIR | (hiRank[i] << RANK_SHIFT_4) | (hiUpTo5Ranks[ranks ^ i] << RANK_SHIFT_1);
				return PAIR | (hiRank[j] << RANK_SHIFT_4) | (hiUpTo5Ranks[ranks ^ j] << RANK_SHIFT_1);
			}
			/* AAAABCD -- one pair (A); j has A's bit */
			return PAIR | (hiRank[j] << RANK_SHIFT_4) | (hiUpTo5Ranks[i] << RANK_SHIFT_1);

		case 5:
			return NO_PAIR |  hiUpTo5Ranks[ranks];

		case 6:
			i = ranks ^ (1 << hiRank[ranks]);
			return NO_PAIR |  hiUpTo5Ranks[i];

		case 7:
			i = ranks ^ (1 << hiRank[ranks]);
			i ^= (1 << hiRank[i]);
			return NO_PAIR |  hiUpTo5Ranks[i];

		} /* end switch */

	    return 0; /* never reached, but avoids compiler warning */
	}

	private static int flushAndOrStraight6(final int ranks, final int c, final int d, final int h, final int s) {

		int	i, j;
		
		if ((j = nbrOfRanks[c]) > 6 - 5) {
			// there's either a club flush or no flush
			if (j >= 5)
				if ((i = straightValue[c]) == 0)
					return FLUSH | hiUpTo5Ranks[c];
				else
					return (STRAIGHT_FLUSH - STRAIGHT) + i;
		} else if ((j += (i = nbrOfRanks[d])) > 6 - 5) {
			if (i >= 5)
				if ((i = straightValue[d]) == 0)
					return FLUSH | hiUpTo5Ranks[d];
				else
					return (STRAIGHT_FLUSH - STRAIGHT) + i;
		} else if ((j += (i = nbrOfRanks[h])) > 6 - 5) {
			if (i >= 5)
				if ((i = straightValue[h]) == 0)
					return FLUSH | hiUpTo5Ranks[h];
				else
					return (STRAIGHT_FLUSH - STRAIGHT) + i;
		} else
			/* total cards in other suits <= N-5: spade flush: */
			if ((i = straightValue[s]) == 0)
				return FLUSH | hiUpTo5Ranks[s];
			else
				return (STRAIGHT_FLUSH - STRAIGHT) + i;
		return straightValue[ranks];
	}

	/**
	 * Returns the value of the best 5-card high poker hand from 6 cards.
	 * @param hand bit mask with one bit set for each of 6 cards.
	 * @return the value of the best 5-card high poker hand.
	 */
	public static int hand6Eval(long hand) {

		final int c = (int)hand & 0x1FFF;
		final int d = ((int)hand >>> 16) & 0x1FFF;
		final int h = (int)(hand >>> 32) & 0x1FFF;
		final int s = (int)(hand >>> 48) & 0x1FFF;

		final int ranks = c | d | h | s;
		int i, j;

	    switch (nbrOfRanks[ranks]) {

	        case 2: /* quads with pair kicker,
					   or two trips (full house) */
					/* bits for trips, if any: */
	                if ((nbrOfRanks[i = c ^ d ^ h ^ s]) != 0)
	                    /* two trips (full house) */
	                	return FULL_HOUSE | (hiUpTo5Ranks[i] << RANK_SHIFT_3);
					/* quads with pair kicker */
	                i = c & d & h & s;  /* bit for quads */
	                return FOUR_OF_A_KIND | (hiRank[i] << RANK_SHIFT_4) | (hiRank[i ^ ranks] << RANK_SHIFT_3);

			case 3:	/* quads with singleton kicker and non-playing singleton,
					   or full house with non-playing singleton,
					   or two pair with non-playing pair */
					if ((c ^ d ^ h ^ s) == 0)
						/* no trips or singletons:  three pair */
						return TWO_PAIR | (hiUpTo5Ranks[ranks] << RANK_SHIFT_2);
					if ((i = c & d & h & s) == 0) {
						/* full house with singleton */
						if ((i = c & d & h) == 0)
							if ((i = c & d & s) == 0)
								if ((i = c & h & s) == 0)
									i = d & h & s; /* bit of trips */
						j = c ^ d ^ h ^ s; /* the bits of the trips and singleton */
						return FULL_HOUSE | (hiRank[i] << RANK_SHIFT_4) | (hiRank[j ^ ranks] << RANK_SHIFT_3); }
					/* quads with kicker and singleton */
					return FOUR_OF_A_KIND | (hiRank[i] << RANK_SHIFT_4) | (hiRank[i ^ ranks] << RANK_SHIFT_3);

			case 4:	/* trips and three singletons,
					   or two pair and two singletons */
					if ((i = c ^ d ^ h ^ s) != ranks)
						/* two pair and two singletons */
						return TWO_PAIR | (hiUpTo5Ranks[i ^ ranks] << RANK_SHIFT_3) | (hiRank[i] << RANK_SHIFT_2);
					/* trips and three singletons */
					if ((i = c & d) == 0)
						i = h & s; /* bit of trips */
					return THREE_OF_A_KIND | (hiRank[i] << RANK_SHIFT_4) | ((hiUpTo5Ranks[ranks ^ i] & 0x00FF0) << RANK_SHIFT_1);

			case 5:	/* flush and/or straight,
					   or one pair and three kickers and
					    one non-playing singleton */
					if ((i = flushAndOrStraight6(ranks, c, d, h, s)) != 0)
						return i;
	                i = c ^ d ^ h ^ s; /* the bits of the four singletons */
	                return PAIR | (hiRank[ i ^ ranks] << RANK_SHIFT_4) | (hiUpTo5Ranks[i] & 0x0FFF0);

			case 6:	/* flush and/or straight or no pair */
					if ((i = flushAndOrStraight6(ranks, c, d, h, s)) != 0)
						return i;
	                return NO_PAIR |  hiUpTo5Ranks[ranks];

	        } /* end switch */

	    return 0; /* never reached, but avoids compiler warning */
	}

	/**
	 * Returns the value of a 5-card poker hand.
	 * @param hand bit mask with one bit set for each of 5 cards.
	 * @return the value of the hand.
	 */
	public static int hand5Eval(long hand) {
	
		final int c = (int)hand & 0x1FFF;
		final int d = ((int)hand >>> 16) & 0x1FFF;
		final int h = (int)(hand >>> 32) & 0x1FFF;
		final int s = (int)(hand >>> 48) & 0x1FFF;

		final int ranks = c | d | h | s;
		int i;

		switch (nbrOfRanks[ranks]) {

	        case 2: /* quads or full house */
	                i = c & d;				/* any two suits */
	                if ((i & h & s) == 0) { /* no bit common to all suits */
	                    i = c ^ d ^ h ^ s;  /* trips bit */
	                    return FULL_HOUSE | (hiRank[i] << RANK_SHIFT_4) | (hiRank[i ^ ranks] << RANK_SHIFT_3); }
	                else
	                    /* the quads bit must be present in each suit mask,
	                       but the kicker bit in no more than one; so we need
	                       only AND any two suit masks to get the quad bit: */
	                    return FOUR_OF_A_KIND | (hiRank[i] << RANK_SHIFT_4) | (hiRank[i ^ ranks] << RANK_SHIFT_3);

	        case 3: /* trips and two kickers,
	                   or two pair and kicker */
	                if ((i = c ^ d ^ h ^ s) == ranks) {
	                    /* trips and two kickers */
	                    if ((i = c & d) == 0)
	                    	if ((i = c & h) == 0)
	                    			i = d & h;
	                    return THREE_OF_A_KIND | (hiRank[i] << RANK_SHIFT_4) 
	                        | (hiUpTo5Ranks[i ^ ranks] << RANK_SHIFT_2); }
	                /* two pair and kicker; i has kicker bit */
	                return TWO_PAIR | (hiUpTo5Ranks[i ^ ranks] << RANK_SHIFT_3) | (hiRank[i] << RANK_SHIFT_2);

	        case 4: /* pair and three kickers */
	                i = c ^ d ^ h ^ s; /* kicker bits */
	                return PAIR | (hiRank[ranks ^ i] << RANK_SHIFT_4) | (hiUpTo5Ranks[i] << RANK_SHIFT_1);

	        case 5: /* flush and/or straight, or no pair */
					if ((i = straightValue[ranks]) == 0)
						i = hiUpTo5Ranks[ranks];
					if (c != 0) {			/* if any clubs... */
						if (c != ranks)		/*   if no club flush... */
							return i; }		/*      return straight or no pair value */
					else
						if (d != 0) {
							if (d != ranks)
								return i; }
						else
							if (h != 0) {
								if (h != ranks)
									return i; }
						/*	else s == ranks: spade flush */
					/* There is a flush */
					if (i < STRAIGHT)
						/* no straight */
						return FLUSH | i;
					else
						return (STRAIGHT_FLUSH - STRAIGHT) + i;
		}

	    return 0; /* never reached, but avoids compiler warning */
	}

	/**
	 * Returns the Deuce-to-Seven low (Kansas City lowball) value of a 5-card poker hand.
	 * @param hand bit mask with one bit set for each of 5 cards.
	 * @return the value of the hand.
	 */
	public static int hand2to7LoEval(long hand) {

		final int WHEEL_EVAL		= 0x04030000;
		final int WHEEL_FLUSH_EVAL	= 0x08030000;
		final int NO_PAIR_ACE_HIGH	= 0x000C3210;
		
		int	result = hand5Eval(hand);
		if (result == WHEEL_EVAL)
			return NO_PAIR_ACE_HIGH;
		if (result == WHEEL_FLUSH_EVAL)
			return FLUSH | NO_PAIR_ACE_HIGH;
		return result;
		
	}

	/**
	 * Returns the Ace-to-5 value of a 5-card low poker hand.
	 * @param hand bit mask with one bit set for each of 5 cards.
	 * @return the Ace-to-5 low value of the hand.
	 */
	public static int handAto5LoEval(long hand) {

		// each of the following extracts a 13-bit field from hand and
		// rotates it left to position the ace in the least significant bit
		final int c = (((int)hand & 0x0FFF) << 1)  + (((int)hand & 0x1000) >> 12);
		final int d = (((int)hand >> 15) & 0x1FFE) + (((int)hand & (0x1000 << 16)) >> 28);
		final int h = ((int)(hand >> 31) & 0x1FFE) + (int)((hand & (0x1000L << 32)) >> 44);
		final int s = ((int)(hand >> 47) & 0x1FFE) + (int)((hand & (0x1000L << 48)) >> 60);

		final int ranks = c | d | h | s;
		int i;

		switch (nbrOfRanks[ranks]) {

	        case 2: /* quads or full house */
	                i = c & d;				/* any two suits */
	                if ((i & h & s) == 0) { /* no bit common to all suits */
	                    i = c ^ d ^ h ^ s;  /* trips bit */
	                    return FULL_HOUSE | (hiRank[i] << RANK_SHIFT_4) | (hiRank[i ^ ranks] << RANK_SHIFT_3); }
	                else
	                    /* the quads bit must be present in each suit mask,
	                       but the kicker bit in no more than one; so we need
	                       only AND any two suit masks to get the quad bit: */
	                    return FOUR_OF_A_KIND | (hiRank[i] << RANK_SHIFT_4) | (hiRank[i ^ ranks] << RANK_SHIFT_3);

	        case 3: /* trips and two kickers,
	                   or two pair and kicker */
	                if ((i = c ^ d ^ h ^ s) == ranks) {
	                    /* trips and two kickers */
	                    if ((i = c & d) == 0)
	                    	if ((i = c & h) == 0)
	                    		i = d & h;
	                    return THREE_OF_A_KIND | (hiRank[i] << RANK_SHIFT_4) 
	                        | (hiUpTo5Ranks[i ^ ranks] << RANK_SHIFT_2); }
	                /* two pair and kicker; i has kicker bit */
	                return TWO_PAIR | (hiUpTo5Ranks[i ^ ranks] << RANK_SHIFT_3) | (hiRank[i] << RANK_SHIFT_2);

	        case 4: /* pair and three kickers */
	                i = c ^ d ^ h ^ s; /* kicker bits */
	                return PAIR | (hiRank[ranks ^ i] << RANK_SHIFT_4) | (hiUpTo5Ranks[i] << RANK_SHIFT_1);

	        case 5: /* no pair */
					return hiUpTo5Ranks[ranks];
		}

	    return 0; /* never reached, but avoids compiler warning */
	}

	/**
	 * Returns the bitwise OR of the suit masks comprising <code>hand</code>; Ace is high.
	 * @param hand bit mask with one bit set for each of 0 to 52 cards.
	 * @return the bitwise OR of the suit masks comprising <code>hand</code>.
	 */
	public static int ranksMask(long hand) {
		
		return (	((int)hand & 0x1FFF)
				|	(((int)hand >>> 16) & 0x1FFF)
				|	((int)(hand >>> 32) & 0x1FFF)
				|	((int)(hand >>> 48) & 0x1FFF)
			   );		
	}

	/**
	 * Returns the bitwise OR of the suit masks comprising <code>hand</code>; Ace is low.
	 * @param hand bit mask with one bit set for each of 0 to 52 cards.
	 * @return the bitwise OR of the suit masks comprising <code>hand</code>.
	 */
	public static int ranksMaskLo(long hand) {
		
		return (	((((int)hand & 0x0FFF) << 1)  + (((int)hand & 0x1000) >> 12))
				|	((((int)hand >> 15) & 0x1FFE) + (((int)hand & (0x1000  << 16)) >> 28))
				|	(((int)(hand >> 31) & 0x1FFE) + (int)((hand & (0x1000L << 32)) >> 44))
				|	(((int)(hand >> 47) & 0x1FFE) + (int)((hand & (0x1000L << 48)) >> 60))
			   );		
	}

	/**
	 * Returns the 8-or-better low value of a 5-card poker hand or {@link #NO_8_LOW}.
	 * @param hand bit mask with one bit set for each of up to 7 cards.
	 * @return the 8-or-better low value of <code>hand</code> or {@link #NO_8_LOW}.
	 */
	public static int hand8LowEval(long hand) {
		
		int result = loMaskOrNo8Low[ranksMaskLo(hand)];
		return result == NO_8_LOW ? NO_8_LOW : hiUpTo5Ranks[result];
	}

	private static int Omaha8LowMaskEval(int twoHolesMask, int boardMask) {
	    return loMaskOrNo8Low[lo3_8OBRanksMask[boardMask & ~twoHolesMask] | twoHolesMask];
	}

	/**
	 * Returns the 8-or-better low value of a 5-card poker hand comprised of three board
	 * cards and two hole cards or {@link #NO_8_LOW}.
	 * @param holeCards CardSet of four hole cards.
	 * @param boardCards CardSet of at least three board cards.
	 * @return the 8-or-better low value or {@link #NO_8_LOW}.
	 */
	/*
	public static int Omaha8LowEval(CardSet holeCards, CardSet boardCards) {
		
		int board = ranksMaskLo(encode(boardCards));
		if (lo3_8OBRanksMask[board] == 0)
			return NO_8_LOW;
		int hole8OB[] = new int[4];
		int i, hole8OBCount = 0;
		for (Card c : holeCards)
			if ((i = ranksMaskLo(encode(c))) <= 0x0080)	// hole card rank <= 8?
				hole8OB[hole8OBCount++] = i;
		int result = NO_8_LOW;
		if (hole8OBCount >= 2) {
    		if ((i = Omaha8LowMaskEval(hole8OB[0] | hole8OB[1], board)) < result)
    				result = i;
    		if (hole8OBCount >= 3 ) {
        		if ((i = Omaha8LowMaskEval(hole8OB[0] | hole8OB[2], board)) < result)
        			result = i;
        		if ((i = Omaha8LowMaskEval(hole8OB[1] | hole8OB[2], board)) < result)
        			result = i;
        		if (hole8OBCount == 4) {
            		if ((i = Omaha8LowMaskEval(hole8OB[0] | hole8OB[3], board)) < result)
            			result = i;
            		if ((i = Omaha8LowMaskEval(hole8OB[1] | hole8OB[3], board)) < result)
            			result = i;
            		if ((i = Omaha8LowMaskEval(hole8OB[2] | hole8OB[3], board)) < result)
            			result = i;
        		}
    		}
		}
		return result == NO_8_LOW ? NO_8_LOW : hiUpTo5Ranks[result];
	}
	*/

// The following exports of accessors to arrays used by the
// evaluation routines may be uncommented if needed.
	
//	/**
//	 * Returns the number of bits set in mask.
//	 * @param mask an int in the range 0..0x1FC0 (8128).
//	 * @return the number of bits set in mask.
//	 * @throws IndexOutOfBoundsException if mask < 0 || mask > 0x1FC0.
//	 */
//	public static int numberOfRanks(int mask)
//	{
//	    return nbrOfRanks[mask];
//	}
//
//	/**
//	 * Returns the rank (2..14) corresponding to the high-order bit set in mask.
//	 * @param mask an int in the range 0..0x1FC0 (8128).
//	 * @return the rank (2..14) corresponding to the high-order bit set in mask.
//	 * @throws IndexOutOfBoundsException if mask < 0 || mask > 0x1FC0.
//	 */
//	public static int rankOfHiCard(int mask)
//	{
//	    return hiRank[mask] + 2;
//	}

	/** ********** Initialization ********************** */

	private static final int ACE_RANK	= 14;

	private static final int A5432		= 0x0000100F; // A5432

	// initializer block
	static {
		int mask, bitCount, ranks;
		int shiftReg, i;
		int value;

		for (mask = 1; mask < ARRAY_SIZE; ++mask) {
			bitCount = ranks = 0;
			shiftReg = mask;
			for (i = ACE_RANK - 2; i >= 0; --i, shiftReg <<= 1)
				if ((shiftReg & 0x1000) != 0)
					if (++bitCount <= 5) {
						ranks <<= RANK_SHIFT_1;
						ranks += i;
						if (bitCount == 1)
							hiRank[mask] = i;
					}
			hiUpTo5Ranks[mask] = ranks;
			nbrOfRanks[mask] = bitCount;

			loMaskOrNo8Low[mask] = NO_8_LOW;
			bitCount = value = 0;
			shiftReg = mask;
			/* For the purpose of this loop, Ace is low; it's in the LS bit */
			for (i = 0; i < 8; ++i, shiftReg >>= 1)
				if ((shiftReg & 1) != 0) {
					value |= (1 << i); /* undo previous shifts, copy bit */
					if (++bitCount == 3)
						lo3_8OBRanksMask[mask] = value;
					if (bitCount == 5) {
						loMaskOrNo8Low[mask] = value;
						break; }
				}
		}
		for (mask = 0x1F00/* A..T */; mask >= 0x001F/* 6..2 */; mask >>= 1)
			setStraight(mask);
		setStraight(A5432); /* A,5..2 */
	}

	private static void setStraight(int ts) {
		/* must call with ts from A..T to 5..A in that order */

			int es, i, j;

			for (i = 0x1000; i > 0; i >>= 1)
				for (j = 0x1000; j > 0; j >>= 1) {
					es = ts | i | j; /* 5 straight bits plus up to two other bits */
					if (straightValue[es] == 0)
						if (ts == A5432)
							straightValue[es] = STRAIGHT | ((5-2) << RANK_SHIFT_4);
						else
							straightValue[es] = STRAIGHT | (hiRank[ts] << RANK_SHIFT_4);
				}
		}
}
