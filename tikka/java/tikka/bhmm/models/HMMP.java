///////////////////////////////////////////////////////////////////////////////
//  Copyright (C) 2010 Taesun Moon, The University of Texas at Austin
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 3 of the License, or (at your option) any later version.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public
//  License along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
///////////////////////////////////////////////////////////////////////////////
package tikka.bhmm.models;

import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;

import tikka.bhmm.apps.CommandLineOptions;
import tikka.utils.annealer.Annealer;
import tikka.bhmm.model.base.HMMBase;
import tikka.structures.*;


/**
 * The HMM+ model in the papers
 *
 * @author tsmoon
 */
public class HMMP extends HMM {

    public HMMP(CommandLineOptions options) {
        super(options);
    }

    /**
     * Training routine for the inner iterations
     */
    @Override
    protected void trainInnerIter(int itermax, Annealer annealer) {
        int wordid, stateid;
        int prev = (stateS-1), current = (stateS-1), next = (stateS-1), nnext = (stateS-1);
        double max = 0, totalprob = 0;
        double r = 0;
        int wordstateoff;
        int pprevsentid = -1;
        int prevsentid = -1;
        int nextsentid = -1;
        int nnextsentid = -1;

        long start = System.currentTimeMillis();
        for (int iter = 0; iter < itermax; ++iter) {
            System.err.println("\n\niteration " + iter + " (Elapsed time = +" +
                (System.currentTimeMillis()-start)/1000 + "s)");
            current = stateS-1;
            prev = stateS-1;
            System.err.print("Number of words processed: ");
            for (int i = 0; i < wordN; i++) {
                if (i % 100000 == 0) {
                    System.err.print(((float)i/1000000) + "M, ");
                }
                wordid = wordVector[i];
                stateid = stateVector[i];
                wordstateoff = stateS * wordid;

                stateByWord[wordstateoff + stateid]--;
                stateCounts[stateid]--;
                firstOrderTransitions[first[i] * stateS + stateid]--;
                secondOrderTransitions[(second[i]*S2) + (first[i]*stateS) + stateid]--;

                /*
                System.out.println("new counts after decrements:");
                System.out.println("StateCounts = " + Arrays.toString(stateCounts));
                System.out.println("StateByWord = " + Arrays.toString(stateByWord));
                System.out.println("first = " + Arrays.toString(first));
                System.out.println("second = " + Arrays.toString(second));
                System.out.println("firstOrderTransitions = " +
                    Arrays.toString(firstOrderTransitions));
                System.out.println("secondOrderTransitions = "+
                    Arrays.toString(secondOrderTransitions));
                */

                //update next and nnext
                try {
                    next = stateVector[i + 1];
                    nextsentid = sentenceVector[i + 1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    next = stateS-1;
                    nextsentid = -1;
                }
                try {
                    nnext = stateVector[i + 2];
                    nnextsentid = sentenceVector[i + 2];
                } catch (ArrayIndexOutOfBoundsException e) {
                    nnext = stateS-1;
                    nnextsentid = -1;
                }
                // update current, prev, next and nnext
                if (sentenceVector[i] != prevsentid) {
                    current = stateS-1;
                    prev = stateS-1;
                }  else if (sentenceVector[i] != pprevsentid) {
                    prev = stateS-1;
                }

                if (sentenceVector[i] != nextsentid) {
                    next = stateS-1;
                    nnext = stateS-1;
                }  else if (sentenceVector[i] != nnextsentid) {
                    nnext = stateS-1;
                }


                for (int j=0; j < (stateS-1); j++) {
                    // see words as 'abxcd', where x is the current word
                    double x = 0.0;
                    if (j<stateC) {
                        x = (stateByWord[wordstateoff + j] + beta) /
                            (stateCounts[j] + wbeta);
                    } else {
                        x = (stateByWord[wordstateoff + j] + delta) /
                            (stateCounts[j] + wdelta);
                    }
                    double abx =
                            (secondOrderTransitions[(prev*S2+current*stateS+j)]+gamma);
                    double bxc =
                            (secondOrderTransitions[(current*S2+j*stateS+next)]+gamma) /
                            (firstOrderTransitions[current*stateS + j] + sgamma);
                    double xcd =
                            (secondOrderTransitions[(j*S2+next*stateS+nnext)]+gamma) /
                            (firstOrderTransitions[j*stateS + next] + sgamma);
                    stateProbs[j] = x*abx*bxc*xcd;
                }
                totalprob = annealer.annealProbs(stateProbs);
                r = mtfRand.nextDouble() * totalprob;
                stateid = 0;
                max = stateProbs[stateid];
                while (r > max) {
                    stateid++;
                    max += stateProbs[stateid];
                }
                stateVector[i] = stateid;

                stateByWord[wordstateoff + stateid]++;
                stateCounts[stateid]++;
                firstOrderTransitions[current*stateS + stateid]++;
                secondOrderTransitions[prev*S2 + current*stateS+ stateid]++;
                first[i] = current;
                second[i] = prev;
                prev = current;
                current = stateid;
                pprevsentid = prevsentid;
                prevsentid = sentenceVector[i];
            }
            /*
            System.out.println("End of iteration:");
            System.out.println("WordVector = " + Arrays.toString(wordVector));
            System.out.println("SentenceVector = " + Arrays.toString(sentenceVector));
            System.out.println("StateVector = " + Arrays.toString(stateVector));
            System.out.println("StateCounts = " + Arrays.toString(stateCounts));
            System.out.println("StateByWord = " + Arrays.toString(stateByWord));
            System.out.println("first = " + Arrays.toString(first));
            System.out.println("second = " + Arrays.toString(second));
            System.out.println("firstOrderTransitions = " + Arrays.toString(firstOrderTransitions));
            System.out.println("secondOrderTransitions = "+Arrays.toString(secondOrderTransitions));
            System.out.println("----------------------------------------------------------");
            */
        }
    }

    /**
     * Randomly initialize learning parameters
     */
    @Override
    public void initializeParametersRandom() {
        int wordid, stateid;
        int prev = (stateS-1), current = (stateS-1);
        double max = 0, totalprob = 0;
        double r = 0;
        int wordstateoff, stateoff, secondstateoff;

        /**
         * Initialize by assigning random topic indices to words
         */
        for (int i = 0; i < wordN; ++i) {
            wordid = wordVector[i];
            wordstateoff = stateS * wordid;

            totalprob = 0;
            stateoff = current * stateS;
            secondstateoff = (prev*S2) + (current*stateS);
            if (mtfRand.nextDouble() > 0.5) {
                for (int j = 0; j < stateC; j++) {
                    totalprob += stateProbs[j] = 1.0;
                }
                stateid = 0;
            } else {
                for (int j = stateC; j < (stateS-1); j++) {
                    totalprob += stateProbs[j] = 1.0;
                }
                r = mtfRand.nextDouble() * totalprob;
                stateid = stateC;
            }
            r = mtfRand.nextDouble() * totalprob;
            max = stateProbs[stateid];
            while (r > max) {
                stateid++;
                max += stateProbs[stateid];
            }
            stateVector[i] = stateid;
            firstOrderTransitions[stateoff + stateid]++;
            secondOrderTransitions[secondstateoff + stateid]++;
            stateByWord[wordstateoff + stateid]++;
            stateCounts[stateid]++;
            first[i] = current;
            second[i] = prev;
            prev = current;
            current = stateid;
        }
        /*
        System.out.println("After initialisation:");
        System.out.println("WordVector = " + Arrays.toString(wordVector));
        System.out.println("SentenceVector = " + Arrays.toString(sentenceVector));
        System.out.println("StateVector = " + Arrays.toString(stateVector));
        System.out.println("StateCounts = " + Arrays.toString(stateCounts));
        System.out.println("StateByWord = " + Arrays.toString(stateByWord));
        System.out.println("first = " + Arrays.toString(first));
        System.out.println("second = " + Arrays.toString(second));
        System.out.println("firstOrderTransitions = " + Arrays.toString(firstOrderTransitions));
        System.out.println("secondOrderTransitions = "+Arrays.toString(secondOrderTransitions));
        System.out.println("----------------------------------------------------------");*/
    }

    /** 
     * Normalize the sample counts for words given state.
     */
    @Override
    protected void normalizeStates() {
        topWordsPerState = new StringDoublePair[stateS][];
        for (int i = 0; i < stateS; ++i) {
            topWordsPerState[i] = new StringDoublePair[outputPerClass];
        }   

        double sum = 0.; 
        for (int i = 0; i < stateS; ++i) {
            if (i<stateC) {
                stateProbs[i] = stateCounts[i] + wbeta;
            } else {
                stateProbs[i] = stateCounts[i] + wdelta;
            }
            sum += stateProbs[i];
            ArrayList<DoubleStringPair> topWords =
                  new ArrayList<DoubleStringPair>();
            /** 
             * Start at one to leave out EOSi
             */
//            for (int j = EOSi + 1; j < wordW; ++j) {
            for (int j = 0; j < wordW; ++j) {
                double prior = 0.0;
                if (i < stateC) {
                    prior = beta;
                } else {
                    prior = delta;
                }
                topWords.add(new DoubleStringPair(
                      stateByWord[j * stateS + i] + prior, trainIdxToWord.get(
                      j)));
            }   
            Collections.sort(topWords);
            for (int j = 0; j < outputPerClass; ++j) {
                if (j < topWords.size()) {
                    topWordsPerState[i][j] =
                        new StringDoublePair(
                        topWords.get(j).stringValue,
                        topWords.get(j).doubleValue / stateProbs[i]);
                } else {
                    topWordsPerState[i][j] =
                        new StringDoublePair("Null", 0.0);
                }   
            }   
        }   

        for (int i = 0; i < stateS; ++i) {
            stateProbs[i] /= sum;
        }   
    } 

}
