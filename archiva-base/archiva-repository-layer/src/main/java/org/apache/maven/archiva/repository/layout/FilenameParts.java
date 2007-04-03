package org.apache.maven.archiva.repository.layout;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * FilenameParts - data object for {@link RepositoryLayoutUtils#splitFilename(String, String)} method. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
class FilenameParts
{
    public String artifactId;

    public String version;

    public String classifier;

    public String extension;

    public void appendArtifactId( String piece )
    {
        if ( artifactId == null )
        {
            artifactId = piece;
        }
        else
        {
            artifactId += "-" + piece;
        }
    }

    public void appendVersion( String piece )
    {
        if ( version == null )
        {
            version = piece;
        }
        else
        {
            version += "-" + piece;
        }
    }

    public void appendClassifier( String piece )
    {
        if ( classifier == null )
        {
            classifier = piece;
        }
        else
        {
            classifier += "-" + piece;
        }
    }
}