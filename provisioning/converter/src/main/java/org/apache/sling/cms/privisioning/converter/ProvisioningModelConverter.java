/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.cms.feature;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.modelconverter.FeatureToProvisioning;
import org.apache.sling.maven.slingstart.ModelUtils;

@Mojo(name = "fm-to-pm", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class ProvisioningModelConverter extends AbstractMojo {

    @Component
    private MavenSession session;

    @Component
    private MavenProject project;

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    @Component
    private ArtifactResolver artifactResolver;

    @Parameter
    private File featureModel;

    @Parameter
    private File outputFile;

    public Feature getFeature(ArtifactId artifact) {
        getLog().debug("Resolving artifact: "+ artifact.toString());
        try {
            File file = ModelUtils.getArtifact(project, session, artifactHandlerManager, artifactResolver,
                    artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(),
                    artifact.getClassifier()).getFile();
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                return FeatureJSONReader.read(reader, file.toURI().toURL().toString());
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get artifact " + artifact, ex);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!featureModel.exists()) {
            throw new MojoExecutionException("Input file " + featureModel + " not found");
        } else {
            getLog().info("Reading feature model from: "+ featureModel.getAbsolutePath());
        }
        if(!outputFile.getParentFile().exists()){
            getLog().info("Creating output directory: "+ outputFile.getParentFile().getAbsolutePath());
            outputFile.getParentFile().mkdirs();
        }
        getLog().info("Writing provisioning model to: "+ outputFile.getAbsolutePath());

        FeatureToProvisioning.convert(featureModel, outputFile, artifact -> getFeature(artifact));
    }

}
