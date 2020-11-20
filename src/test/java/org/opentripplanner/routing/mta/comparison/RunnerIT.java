/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.opentripplanner.routing.mta.comparison;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.opentripplanner.routing.mta.comparison.GenerateTestODPairsFromRunningInstance;

import java.io.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunnerIT {
	
    @Test
    public void a_ODGen_QA_NoAccessible() throws IOException, Exception {    	
		
    	GenerateTestODPairsFromRunningInstance t = new GenerateTestODPairsFromRunningInstance();
    	t.setOTPURL("http://otp-mta-qa.camsys-apps.com/otp/routers/default/index/stops?apikey=EQVQV8RM6R4o3Dwb6YNWfg6OMSR7kT9L");
    	t.setMTAOnly(true);

    	t.setPairsToGenerate(150);
    	t.setAccessibilityPercent(0.0);
    	t.setMax("MTA", 50);
    	t.setMax("MTASBWY", 50);
    	t.setMax("LI", 25);
    	t.setMax("MNR", 25);
    	
    	t.setOutputFile("src/test/resources/mta/test_pairs_0_accessible.txt");
    	t.run();
    }
    
    @Test    
    public void b_ODGen_QA_Accessible() throws IOException, Exception {    	

    	GenerateTestODPairsFromRunningInstance t = new GenerateTestODPairsFromRunningInstance();
    	t.setOTPURL("http://otp-mta-qa.camsys-apps.com/otp/routers/default/index/stops?apikey=EQVQV8RM6R4o3Dwb6YNWfg6OMSR7kT9L");
    	t.setMTAOnly(true);

    	t.setPairsToGenerate(150);
    	t.setAccessibilityPercent(1.0);
    	t.setMax("MTA", 50);
    	t.setMax("MTASBWY", 50);
    	t.setMax("LI", 25);
    	t.setMax("MNR", 25);
    	
    	t.setOutputFile("src/test/resources/mta/test_pairs_100_accessible.txt");
    	t.run();
    }
    
    @Test  	
    public void c_Baseline_QA_NoAccessible() throws IOException, Exception {    	
    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-qa.camsys-apps.com/otp/routers/default/plan?apikey=EQVQV8RM6R4o3Dwb6YNWfg6OMSR7kT9L");
    	t2.setInputFile("src/test/resources/mta/test_pairs_0_accessible.txt");
    	t2.setOutputFile("src/test/resources/mta/test_qa_0_accessible.txt");
    	t2.run();    	
    }
    
    @Test
    public void d_Baseline_QA_Accessible() throws IOException, Exception {    	
    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-qa.camsys-apps.com/otp/routers/default/plan?apikey=EQVQV8RM6R4o3Dwb6YNWfg6OMSR7kT9L");
    	t2.setInputFile("src/test/resources/mta/test_pairs_100_accessible.txt");
    	t2.setOutputFile("src/test/resources/mta/test_qa_100_accessible.txt");
    	t2.run();    	
    }
 	
    @Test
    public void e_Baseline_Prod_NoAccessible() throws IOException, Exception {    	
    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-prod.camsys-apps.com/otp/routers/default/plan?apikey=Z276E3rCeTzOQEoBPPN4JCEc6GfvdnYE");
    	t2.setInputFile("src/test/resources/mta/test_pairs_0_accessible.txt");
    	t2.setOutputFile("src/test/resources/mta/test_prod_0_accessible.txt");
    	t2.run();    	
    }

    @Test
    public void f_Baseline_Prod_Accessible() throws IOException, Exception {    	
    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-prod.camsys-apps.com/otp/routers/default/plan?apikey=Z276E3rCeTzOQEoBPPN4JCEc6GfvdnYE");
    	t2.setInputFile("src/test/resources/mta/test_pairs_100_accessible.txt");
    	t2.setOutputFile("src/test/resources/mta/test_prod_100_accessible.txt");
    	t2.run();    	
    }
    
    

    
    
	// this blocks the rest from continuing until dev is updated with the code we just committed
	@Test
    public void g_Block_Until_Released() throws IOException, Exception {    	
		
    	BlockUntilReleaseFinished t = new BlockUntilReleaseFinished();
    	t.setOTPURL("https://otp-mta-dev.camsys-apps.com/otp/?apikey=hAR0VMP2Ufxk542WrtTW8ToBmi4N3UUp");
    	t.run();
    }
    
    @Test
    public void h_Test_Dev_NoAccessible() throws IOException, Exception {    	
    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-dev.camsys-apps.com/otp/routers/default/plan?apikey=hAR0VMP2Ufxk542WrtTW8ToBmi4N3UUp");
    	t2.setInputFile("src/test/resources/mta/test_pairs_0_accessible.txt");
    	t2.setOutputFile("src/test/resources/mta/test_dev_0_accessible.txt");
    	t2.run();    	
    }
   
    @Test
    public void i_Test_Dev_Accessible() throws IOException, Exception {    	
    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-dev.camsys-apps.com/otp/routers/default/plan?apikey=hAR0VMP2Ufxk542WrtTW8ToBmi4N3UUp");
    	t2.setInputFile("src/test/resources/mta/test_pairs_100_accessible.txt");
    	t2.setOutputFile("src/test/resources/mta/test_dev_100_accessible.txt");
    	t2.run();    	
    }
    
    
    
    
    
    
    
    
    
    
     
    @Test
    public void l_TestEval_Get_Reference() throws IOException, Exception {    	
    	FetchTripEvaluationReference t2 = new FetchTripEvaluationReference();
    	t2.setOutputQueryFile("src/test/resources/mta/tripeval_od_pairs.txt");
    	t2.setOutputReferenceFile("src/test/resources/mta/tripeval_output_results.txt");
    	t2.run();    	
    }
    
    @Test
    public void m_TestEval_Dev() throws IOException, Exception {    	
    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-dev.camsys-apps.com/otp/routers/default/plan?apikey=hAR0VMP2Ufxk542WrtTW8ToBmi4N3UUp");
    	t2.setInputFile("src/test/resources/mta/tripeval_od_pairs.txt");
    	t2.setOutputFile("src/test/resources/mta/tripeval_dev_results.txt");
    	t2.setUseCurrentTime(true);
    	t2.run();    	
    }
    

    
    
    
    
    
    
    
    @Test
    public void q1_QAXProd_Not_Accessible_Results() throws IOException, Exception {    	
    	System.out.println("             ***************************************************************");
    	System.out.println("                                    PROD   vs   QA - NO ACCESSIBLE          ");
    	System.out.println("             ***************************************************************");
 
    	QualitativeMultiDimInstanceComparison t3 = new QualitativeMultiDimInstanceComparison();
    	t3.setBaselineResultsFile("src/test/resources/mta/test_prod_0_accessible.txt");
    	t3.setTestResultsFile("src/test/resources/mta/test_qa_0_accessible.txt");
    	t3.run();

    }
    
    @Test
    public void q2_QAXProd_Accessible_Results() throws IOException, Exception {    	
    	System.out.println("             ***************************************************************");
    	System.out.println("                                    PROD   vs   QA - ACCESSIBLE          ");
    	System.out.println("             ***************************************************************");
 
    	QualitativeMultiDimInstanceComparison t4 = new QualitativeMultiDimInstanceComparison();
    	t4.setBaselineResultsFile("src/test/resources/mta/test_prod_100_accessible.txt");
    	t4.setTestResultsFile("src/test/resources/mta/test_qa_100_accessible.txt");
    	t4.run();
    }    	
    	
    
    @Test
    public void r1_DevXQA_Not_Accessible_Results() throws IOException, Exception {    	
    	System.out.println("             ***************************************************************");
    	System.out.println("                                    QA   vs   DEV - NO ACCESSIBLE           ");
    	System.out.println("             ***************************************************************");

    	QualitativeMultiDimInstanceComparison t3 = new QualitativeMultiDimInstanceComparison();
    	t3.setBaselineResultsFile("src/test/resources/mta/test_qa_0_accessible.txt");
    	t3.setTestResultsFile("src/test/resources/mta/test_dev_0_accessible.txt");
    	t3.run();
    }
    
    @Test
    public void r2_DevXQA_Accessible_Results() throws IOException, Exception {    	
    	System.out.println("             ***************************************************************");
    	System.out.println("                                    QA   vs   DEV - ACCESSIBLE           ");
    	System.out.println("             ***************************************************************");
    	
    	QualitativeMultiDimInstanceComparison t4 = new QualitativeMultiDimInstanceComparison();
    	t4.setBaselineResultsFile("src/test/resources/mta/test_qa_100_accessible.txt");
    	t4.setTestResultsFile("src/test/resources/mta/test_dev_100_accessible.txt");
    	t4.run();
    }    		

    
    
    
    
    @Test
    public void s_DevXTripEval_Results() throws IOException, Exception {    	
    	System.out.println("             ***************************************************************");
    	System.out.println("                                 TRIPEVAL   vs   DEV                        ");
    	System.out.println("             ***************************************************************");
    	System.out.println("");
    	
      	ScoreAgainstIdealComparison t4 = new ScoreAgainstIdealComparison();
    	t4.setBaselineResultsFile("src/test/resources/mta/tripeval_output_results.txt");
    	t4.setTestResultsFile("src/test/resources/mta/tripeval_dev_results.txt");
    	t4.run();

    } 
}

