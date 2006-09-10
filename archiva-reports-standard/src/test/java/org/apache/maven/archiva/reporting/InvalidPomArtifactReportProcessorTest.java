package org.apache.maven.archiva.reporting;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;

/**
 * This class tests the InvalidPomArtifactReportProcessor class.
 */
public class InvalidPomArtifactReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
    private ArtifactReportProcessor artifactReportProcessor;

    private ReportingDatabase reportDatabase;

    public void setUp()
        throws Exception
    {
        super.setUp();
        artifactReportProcessor = (ArtifactReportProcessor) lookup( ArtifactReportProcessor.ROLE, "invalid-pom" );

        ReportGroup reportGroup = (ReportGroup) lookup( ReportGroup.ROLE, "health" );
        reportDatabase = new ReportingDatabase( reportGroup );
    }

    /**
     * Test the InvalidPomArtifactReportProcessor when the artifact is an invalid pom.
     */
    public void testInvalidPomArtifactReportProcessorFailure()
    {
        Artifact artifact = createArtifact( "org.apache.maven", "artifactId", "1.0-alpha-3", "pom" );

        artifactReportProcessor.processArtifact( artifact, null, reportDatabase );
        assertEquals( 1, reportDatabase.getNumFailures() );
    }


    /**
     * Test the InvalidPomArtifactReportProcessor when the artifact is a valid pom.
     */
    public void testInvalidPomArtifactReportProcessorSuccess()
    {
        Artifact artifact = createArtifact( "groupId", "artifactId", "1.0-alpha-2", "pom" );

        artifactReportProcessor.processArtifact( artifact, null, reportDatabase );
        assertEquals( 0, reportDatabase.getNumFailures() );
        assertEquals( 0, reportDatabase.getNumWarnings() );
    }


    /**
     * Test the InvalidPomArtifactReportProcessor when the artifact is not a pom.
     */
    public void testNotAPomArtifactReportProcessorSuccess()
    {
        Artifact artifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1", "jar" );

        artifactReportProcessor.processArtifact( artifact, null, reportDatabase );
        assertEquals( 0, reportDatabase.getNumFailures() );
        assertEquals( 0, reportDatabase.getNumWarnings() );
    }
}
