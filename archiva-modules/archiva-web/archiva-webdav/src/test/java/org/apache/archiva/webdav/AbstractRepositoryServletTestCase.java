package org.apache.archiva.webdav;

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

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.archiva.webdav.util.MavenIndexerCleaner;
import org.apache.archiva.webdav.util.ReinitServlet;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.app.event.ReferenceInsertionEventHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

/**
 * AbstractRepositoryServletTestCase
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:spring-context.xml",
    "classpath*:/repository-servlet-simple.xml" } )
public abstract class AbstractRepositoryServletTestCase
    extends TestCase
{
    protected static final String REPOID_INTERNAL = "internal";

    protected static final String REPOID_LEGACY = "legacy";

    protected File repoRootInternal;

    protected File repoRootLegacy;


    protected ArchivaConfiguration archivaConfiguration;

    @Inject
    protected ApplicationContext applicationContext;

    protected Logger log = LoggerFactory.getLogger( getClass() );


    protected void saveConfiguration()
        throws Exception
    {
        saveConfiguration( archivaConfiguration );
    }

    protected Tomcat tomcat;

    protected static int port;


    StandardContext context;

    @Before
    public void setUp()
        throws Exception
    {

        super.setUp();

        String appserverBase = new File( "target/appserver-base" ).getAbsolutePath();
        System.setProperty( "appserver.base", appserverBase );

        File testConf = new File( "src/test/resources/repository-archiva.xml" );
        File testConfDest = new File( appserverBase, "conf/archiva.xml" );
        if ( testConfDest.exists() )
        {
            FileUtils.deleteQuietly( testConfDest );
        }
        FileUtils.copyFile( testConf, testConfDest );

        archivaConfiguration = applicationContext.getBean( ArchivaConfiguration.class );

        repoRootInternal = new File( appserverBase, "data/repositories/internal" );
        repoRootLegacy = new File( appserverBase, "data/repositories/legacy" );
        Configuration config = archivaConfiguration.getConfiguration();

        config.getManagedRepositories().clear();

        config.addManagedRepository(
            createManagedRepository( REPOID_INTERNAL, "Internal Test Repo", repoRootInternal, true ) );

        config.addManagedRepository(
            createManagedRepository( REPOID_LEGACY, "Legacy Format Test Repo", repoRootLegacy, "legacy", true ) );

        config.getProxyConnectors().clear();

        config.getRemoteRepositories().clear();

        saveConfiguration( archivaConfiguration );

        CacheManager.getInstance().clearAll();

        applicationContext.getBean( MavenIndexerCleaner.class ).cleanupIndex();


    }

    protected UnauthenticatedRepositoryServlet unauthenticatedRepositoryServlet =
        new UnauthenticatedRepositoryServlet();

    protected void startRepository()
        throws Exception
    {
        /*
        tomcat = new Tomcat();
        tomcat.setBaseDir( System.getProperty( "java.io.tmpdir" ) );
        tomcat.setPort( 0 );

        context = StandardContext.class.cast( tomcat.addContext( "", System.getProperty( "java.io.tmpdir" ) ) );

        ApplicationParameter applicationParameter = new ApplicationParameter();
        applicationParameter.setName( "contextConfigLocation" );
        applicationParameter.setValue( getSpringConfigLocation() );
        context.addApplicationParameter( applicationParameter );

        context.addApplicationListener( ContextLoaderListener.class.getName() );

        context.addApplicationListener( MavenIndexerCleaner.class.getName() );

        Tomcat.addServlet( context, "repository", new UnauthenticatedRepositoryServlet() );
        context.addServletMapping( "/repository/*", "repository" );

        Tomcat.addServlet( context, "reinitservlet", new ReinitServlet() );
        context.addServletMapping( "/reinit/*", "reinitservlet" );

        tomcat.start();

        this.port = tomcat.getConnector().getLocalPort();
        */

        final MockServletContext mockServletContext = new MockServletContext();

        WebApplicationContext webApplicationContext =
            new TestWebapplicationContext( applicationContext, mockServletContext );

        mockServletContext.setAttribute( WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
                                         webApplicationContext );

        MockServletConfig mockServletConfig = new MockServletConfig()
        {
            @Override
            public ServletContext getServletContext()
            {
                return mockServletContext;
            }
        };

        unauthenticatedRepositoryServlet.init( mockServletConfig );

    }


    static class TestWebapplicationContext
        implements WebApplicationContext
    {
        private ApplicationContext applicationContext;

        private ServletContext servletContext;

        TestWebapplicationContext( ApplicationContext applicationContext, ServletContext servletContext )
        {
            this.applicationContext = applicationContext;
        }

        @Override
        public ServletContext getServletContext()
        {
            return servletContext;
        }

        @Override
        public String getId()
        {
            return applicationContext.getId();
        }

        @Override
        public String getApplicationName()
        {
            return applicationContext.getApplicationName();
        }

        @Override
        public String getDisplayName()
        {
            return applicationContext.getDisplayName();
        }

        @Override
        public long getStartupDate()
        {
            return applicationContext.getStartupDate();
        }

        @Override
        public ApplicationContext getParent()
        {
            return applicationContext.getParent();
        }

        @Override
        public AutowireCapableBeanFactory getAutowireCapableBeanFactory()
            throws IllegalStateException
        {
            return applicationContext.getAutowireCapableBeanFactory();
        }

        @Override
        public void publishEvent( ApplicationEvent applicationEvent )
        {
            applicationContext.publishEvent( applicationEvent );
        }

        @Override
        public Environment getEnvironment()
        {
            return applicationContext.getEnvironment();
        }

        @Override
        public BeanFactory getParentBeanFactory()
        {
            return applicationContext.getParentBeanFactory();
        }

        @Override
        public boolean containsLocalBean( String s )
        {
            return applicationContext.containsLocalBean( s );
        }

        @Override
        public boolean containsBeanDefinition( String s )
        {
            return applicationContext.containsBeanDefinition( s );
        }

        @Override
        public int getBeanDefinitionCount()
        {
            return applicationContext.getBeanDefinitionCount();
        }

        @Override
        public String[] getBeanDefinitionNames()
        {
            return applicationContext.getBeanDefinitionNames();
        }

        @Override
        public String[] getBeanNamesForType( Class<?> aClass )
        {
            return applicationContext.getBeanNamesForType( aClass );
        }

        @Override
        public String[] getBeanNamesForType( Class<?> aClass, boolean b, boolean b2 )
        {
            return applicationContext.getBeanNamesForType( aClass, b, b2 );
        }

        @Override
        public <T> Map<String, T> getBeansOfType( Class<T> tClass )
            throws BeansException
        {
            return applicationContext.getBeansOfType( tClass );
        }

        @Override
        public <T> Map<String, T> getBeansOfType( Class<T> tClass, boolean b, boolean b2 )
            throws BeansException
        {
            return applicationContext.getBeansOfType( tClass, b, b2 );
        }

        @Override
        public String[] getBeanNamesForAnnotation( Class<? extends Annotation> aClass )
        {
            return applicationContext.getBeanNamesForAnnotation( aClass );
        }

        @Override
        public Map<String, Object> getBeansWithAnnotation( Class<? extends Annotation> aClass )
            throws BeansException
        {
            return applicationContext.getBeansWithAnnotation( aClass );
        }

        @Override
        public <A extends Annotation> A findAnnotationOnBean( String s, Class<A> aClass )
            throws NoSuchBeanDefinitionException
        {
            return applicationContext.findAnnotationOnBean( s, aClass );
        }

        @Override
        public Object getBean( String s )
            throws BeansException
        {
            return applicationContext.getBean( s );
        }

        @Override
        public <T> T getBean( String s, Class<T> tClass )
            throws BeansException
        {
            return applicationContext.getBean( s, tClass );
        }

        @Override
        public <T> T getBean( Class<T> tClass )
            throws BeansException
        {
            return applicationContext.getBean( tClass );
        }

        @Override
        public Object getBean( String s, Object... objects )
            throws BeansException
        {
            return applicationContext.getBean( s, objects );
        }

        @Override
        public boolean containsBean( String s )
        {
            return applicationContext.containsBean( s );
        }

        @Override
        public boolean isSingleton( String s )
            throws NoSuchBeanDefinitionException
        {
            return applicationContext.isSingleton( s );
        }

        @Override
        public boolean isPrototype( String s )
            throws NoSuchBeanDefinitionException
        {
            return applicationContext.isPrototype( s );
        }

        @Override
        public boolean isTypeMatch( String s, Class<?> aClass )
            throws NoSuchBeanDefinitionException
        {
            return applicationContext.isTypeMatch( s, aClass );
        }

        @Override
        public Class<?> getType( String s )
            throws NoSuchBeanDefinitionException
        {
            return applicationContext.getType( s );
        }

        @Override
        public String[] getAliases( String s )
        {
            return applicationContext.getAliases( s );
        }

        @Override
        public String getMessage( String s, Object[] objects, String s2, Locale locale )
        {
            return applicationContext.getMessage( s, objects, s2, locale );
        }

        @Override
        public String getMessage( String s, Object[] objects, Locale locale )
            throws NoSuchMessageException
        {
            return applicationContext.getMessage( s, objects, locale );
        }

        @Override
        public String getMessage( MessageSourceResolvable messageSourceResolvable, Locale locale )
            throws NoSuchMessageException
        {
            return applicationContext.getMessage( messageSourceResolvable, locale );
        }

        @Override
        public Resource[] getResources( String s )
            throws IOException
        {
            return applicationContext.getResources( s );
        }

        @Override
        public Resource getResource( String s )
        {
            return applicationContext.getResource( s );
        }

        @Override
        public ClassLoader getClassLoader()
        {
            return applicationContext.getClassLoader();
        }
    }

    protected Servlet findServlet( String name )
        throws Exception
    {
        return unauthenticatedRepositoryServlet;
        /*
        Container[] childs = context.findChildren();
        for ( Container container : childs )
        {
            if ( StringUtils.equals( container.getName(), name ) )
            {
                Tomcat.ExistingStandardWrapper esw = Tomcat.ExistingStandardWrapper.class.cast( container );
                Servlet servlet = esw.loadServlet();

                return servlet;
            }
        }
        return null;*/
    }

    protected String getSpringConfigLocation()
    {
        return "classpath*:/META-INF/spring-context.xml,classpath*:spring-context.xml";
    }




    /*
    protected ServletUnitClient getServletUnitClient()
        throws Exception
    {
        if ( servletUnitClient != null )
        {
            return servletUnitClient;
        }
        servletRunner = new ServletRunner( new File( "src/test/resources/WEB-INF/web.xml" ) );

        servletRunner.registerServlet( "/repository/*", UnauthenticatedRepositoryServlet.class.getName() );

        servletUnitClient = servletRunner.newClient();

        return servletUnitClient;
    }*/

    /*
    protected <P extends Page> P page(final String path) throws IOException {
        return newClient().getPage(base.toExternalForm() + "repository/" + path);
    }
    */

    protected static WebClient newClient()
    {
        final WebClient webClient = new WebClient();
        webClient.getOptions().setJavaScriptEnabled( false );
        webClient.getOptions().setCssEnabled( false );
        webClient.getOptions().setAppletEnabled( false );
        webClient.getOptions().setThrowExceptionOnFailingStatusCode( false );
        webClient.setAjaxController( new NicelyResynchronizingAjaxController() );
        return webClient;
    }


    protected WebResponse getWebResponse( String path )
        throws Exception
    {

        //WebClient client = newClient();
        //client.getPage( "http://localhost:" + port + "/reinit/reload" );
        //return client.getPage( "http://localhost:" + port + path ).getWebResponse();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI( path );
        request.addHeader( "User-Agent", "Apache Archiva unit test" );
        request.setMethod( "GET" );
        final MockHttpServletResponse response = execute( request );
        return new WebResponse( null, null, 1 )
        {
            @Override
            public String getContentAsString()
            {
                try
                {
                    return response.getContentAsString();
                }
                catch ( UnsupportedEncodingException e )
                {
                    throw new RuntimeException( e.getMessage(), e );
                }
            }

            @Override
            public int getStatusCode()
            {
                return response.getStatus();
            }
        };
    }

    protected MockHttpServletResponse execute( HttpServletRequest request )
        throws Exception
    {
        MockHttpServletResponse response = new MockHttpServletResponse()
        {
            public String getContentAsString()
                throws UnsupportedEncodingException
            {
                String errorMessage  = getErrorMessage();
                return ( errorMessage != null ) ? errorMessage: super.getContentAsString();
            }
        };
        this.unauthenticatedRepositoryServlet.service( request, response );
        return response;
    }

    public static class GetMethodWebRequest
        extends WebRequest
    {
        String url;

        public GetMethodWebRequest( String url )
            throws Exception
        {
            super( new URL( url ) );
            this.url = url;

        }
    }

    public static class PutMethodWebRequest
        extends WebRequest
    {
        String url;

        public PutMethodWebRequest( String url, InputStream inputStream, String contentType )
            throws Exception
        {
            super( new URL( url ), HttpMethod.PUT );
            this.url = url;

        }


    }

    public static class ServletUnitClient
    {

        AbstractRepositoryServletTestCase abstractRepositoryServletTestCase;

        public ServletUnitClient( AbstractRepositoryServletTestCase abstractRepositoryServletTestCase )
        {
            this.abstractRepositoryServletTestCase = abstractRepositoryServletTestCase;
        }

        public WebResponse getResponse( WebRequest request )
            throws Exception
        {
            return abstractRepositoryServletTestCase.getWebResponse( request.getUrl().getPath() );
        }

        public WebResponse getResource( WebRequest request )
            throws Exception
        {
            return getResponse( request );
        }
    }

    public ServletUnitClient getServletUnitClient()
    {
        return new ServletUnitClient( this );
    }

    @Override
    @After
    public void tearDown()
        throws Exception
    {

        if ( repoRootInternal.exists() )
        {
            FileUtils.deleteDirectory( repoRootInternal );
        }

        if ( repoRootLegacy.exists() )
        {
            FileUtils.deleteDirectory( repoRootLegacy );
        }

        if ( this.tomcat != null )
        {
            this.tomcat.stop();
        }

    }


    protected void assertFileContents( String expectedContents, File repoRoot, String path )
        throws IOException
    {
        File actualFile = new File( repoRoot, path );
        assertTrue( "File <" + actualFile.getAbsolutePath() + "> should exist.", actualFile.exists() );
        assertTrue( "File <" + actualFile.getAbsolutePath() + "> should be a file (not a dir/link/device/etc).",
                    actualFile.isFile() );

        String actualContents = FileUtils.readFileToString( actualFile, Charset.defaultCharset() );
        assertEquals( "File Contents of <" + actualFile.getAbsolutePath() + ">", expectedContents, actualContents );
    }

    protected void assertRepositoryValid( RepositoryServlet servlet, String repoId )
        throws Exception
    {
        ManagedRepository repository = servlet.getRepository( repoId );
        assertNotNull( "Archiva Managed Repository id:<" + repoId + "> should exist.", repository );
        File repoRoot = new File( repository.getLocation() );
        assertTrue( "Archiva Managed Repository id:<" + repoId + "> should have a valid location on disk.",
                    repoRoot.exists() && repoRoot.isDirectory() );
    }

    protected void assertResponseOK( WebResponse response )
    {

        assertNotNull( "Should have recieved a response", response );
        Assert.assertEquals( "Should have been an OK response code", HttpServletResponse.SC_OK,
                             response.getStatusCode() );
    }

    protected void assertResponseOK( WebResponse response, String path )
    {
        assertNotNull( "Should have recieved a response", response );
        Assert.assertEquals( "Should have been an OK response code for path: " + path, HttpServletResponse.SC_OK,
                             response.getStatusCode() );
    }

    protected void assertResponseNotFound( WebResponse response )
    {
        assertNotNull( "Should have recieved a response", response );
        Assert.assertEquals( "Should have been an 404/Not Found response code.", HttpServletResponse.SC_NOT_FOUND,
                             response.getStatusCode() );
    }

    protected void assertResponseInternalServerError( WebResponse response )
    {
        assertNotNull( "Should have recieved a response", response );
        Assert.assertEquals( "Should have been an 500/Internal Server Error response code.",
                             HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatusCode() );
    }

    protected void assertResponseConflictError( WebResponse response )
    {
        assertNotNull( "Should have received a response", response );
        Assert.assertEquals( "Should have been a 409/Conflict response code.", HttpServletResponse.SC_CONFLICT,
                             response.getStatusCode() );
    }

    protected ManagedRepositoryConfiguration createManagedRepository( String id, String name, File location,
                                                                      boolean blockRedeployments )
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.getAbsolutePath() );
        repo.setBlockRedeployments( blockRedeployments );

        return repo;
    }

    protected ManagedRepositoryConfiguration createManagedRepository( String id, String name, File location,
                                                                      String layout, boolean blockRedeployments )
    {
        ManagedRepositoryConfiguration repo = createManagedRepository( id, name, location, blockRedeployments );
        repo.setLayout( layout );
        return repo;
    }

    protected RemoteRepositoryConfiguration createRemoteRepository( String id, String name, String url )
    {
        RemoteRepositoryConfiguration repo = new RemoteRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setUrl( url );
        return repo;
    }

    protected void saveConfiguration( ArchivaConfiguration archivaConfiguration )
        throws Exception
    {
        archivaConfiguration.save( archivaConfiguration.getConfiguration() );
    }


    protected void setupCleanRepo( File repoRootDir )
        throws IOException
    {
        FileUtils.deleteDirectory( repoRootDir );
        if ( !repoRootDir.exists() )
        {
            repoRootDir.mkdirs();
        }
    }

    protected void assertManagedFileNotExists( File repoRootInternal, String resourcePath )
    {
        File repoFile = new File( repoRootInternal, resourcePath );
        assertFalse( "Managed Repository File <" + repoFile.getAbsolutePath() + "> should not exist.",
                     repoFile.exists() );
    }

    protected void setupCleanInternalRepo()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );
    }

    protected File populateRepo( File repoRootManaged, String path, String contents )
        throws Exception
    {
        File destFile = new File( repoRootManaged, path );
        destFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( destFile, contents, Charset.defaultCharset() );
        return destFile;
    }
}
