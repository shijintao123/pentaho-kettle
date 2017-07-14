/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2017 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.ui.repository.repositoryexplorer.controllers;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.repository.RepositoryExtended;
import org.pentaho.di.repository.UserInfo;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIRepositoryDirectory;
import org.pentaho.ui.xul.XulDomContainer;
import org.pentaho.ui.xul.binding.Binding;
import org.pentaho.ui.xul.components.XulPromptBox;
import org.pentaho.ui.xul.containers.XulTree;
import org.pentaho.ui.xul.dom.Document;
import org.pentaho.ui.xul.dom.DocumentFactory;
import org.pentaho.ui.xul.dom.dom4j.ElementDom4J;
import org.pentaho.ui.xul.swt.custom.MessageDialogBase;
import org.pentaho.ui.xul.util.XulDialogCallback;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class BrowseControllerTest {

  private static final String PROMPTBOX = "promptbox";
  private static final String FOLDER_NAME = "New Folder";

  private Document document;
  private Binding directoryBinding;
  private Binding selectedItemsBinding;
  private Map<ObjectId, UIRepositoryDirectory> directoryMap;
  private UIRepositoryDirectory selectedFolder;

  private BrowseController controller;

  @Before
  public void setUp() throws Exception {
    DocumentFactory.registerElementClass( ElementDom4J.class );

    controller = new BrowseController();

    controller.setRepositoryDirectory( mock( UIRepositoryDirectory.class ) );

    directoryMap = new HashMap<>( 8 );
    controller.setDirMap( directoryMap );

    document = mock( Document.class );
    XulDomContainer xulDomContainer = mock( XulDomContainer.class );
    when( xulDomContainer.getDocumentRoot() ).thenReturn( document );
    controller.setXulDomContainer( xulDomContainer );

    UIRepositoryDirectory someDirectory = mock( UIRepositoryDirectory.class );
    selectedFolder = mock( UIRepositoryDirectory.class );
    when( selectedFolder.createFolder( FOLDER_NAME ) ).thenReturn( someDirectory );

    XulTree folderTree = mock( XulTree.class );
    when( folderTree.getSelectedItems() ).thenReturn( Collections.<Object>singleton( selectedFolder ) );
    controller.setFolderTree( folderTree );

    directoryBinding = mock( Binding.class );
    controller.setDirectoryBinding( directoryBinding );

    selectedItemsBinding = mock( Binding.class );
    controller.setSelectedItemsBinding( selectedItemsBinding );
  }

  /*
   * Test for {@link BrowseController#createFolder()}.
   *
   * Given an opened folder creation dialog with the non-empty folder name field.
   *
   * When this prompt dialog is just simply closed by pressing 'x' button, then folder should not be created.
   */
  @Test
  public void shouldNotCreateFolderOnCloseCreationDialog() throws Exception {
    XulPromptBox prompt = new XulPromptBoxMock( XulDialogCallback.Status.CANCEL );
    when( document.createElement( PROMPTBOX ) ).thenReturn( prompt );

    controller.createFolder();

    assertTrue( directoryMap.isEmpty() );
    verify( selectedFolder, never() ).createFolder( anyString() );
    verify( directoryBinding, never() ).fireSourceChanged();
    verify( selectedItemsBinding, never() ).fireSourceChanged();
  }

  /*
   * Test for {@link BrowseController#createFolder()}.
   *
   * Given an opened folder creation dialog with the non-empty folder name field.
   *
   * When this prompt dialog is accepted, then a folder should be created.
   */
  @Test
  public void shouldCreateFolderOnAcceptCreationDialog() throws Exception {
    XulPromptBox prompt = new XulPromptBoxMock( XulDialogCallback.Status.ACCEPT );
    when( document.createElement( PROMPTBOX ) ).thenReturn( prompt );

    controller.createFolder();

    assertFalse( directoryMap.isEmpty() );
    verify( selectedFolder ).createFolder( anyString() );
    verify( directoryBinding ).fireSourceChanged();
    verify( selectedItemsBinding ).fireSourceChanged();
  }


  private static class XulPromptBoxMock extends MessageDialogBase implements XulPromptBox {
    private final XulDialogCallback.Status status;

    public XulPromptBoxMock( XulDialogCallback.Status status ) {
      super( PROMPTBOX );
      this.status = status;
    }

    @Override
    public String getValue() {
      return null;
    }

    @Override
    public void setValue( String value ) {
      // do nothing
    }

    @Override
    public int open() {
      for ( XulDialogCallback<String> callback : callbacks ) {
        callback.onClose( null, status, FOLDER_NAME );
      }
      return 0;
    }
  }

  //PDI-16258
  @Test
  public void testInit() throws Exception {

    RepositoryExtended repository = Mockito.mock( RepositoryExtended.class );

    UserInfo userInfo = new UserInfo( "user", "password", "user", "test user", true );
    userInfo.setAdmin( true );
    Mockito.doReturn( userInfo ).when( repository ).getUserInfo();
    Mockito.doReturn( Mockito.mock( RepositoryDirectoryInterface.class ) ).when( repository ).
            loadRepositoryDirectoryTree( anyString(), anyString(), anyInt(), anyBoolean(), anyBoolean(), anyBoolean() );

    BrowseController browseController = Mockito.spy( controller );

    doNothing().when( browseController ).createBindings();
    browseController.init( repository );
    Mockito.verify( repository  ).loadRepositoryDirectoryTree( "/", "*.ktr|*.kjb", -1, true, true, true );


    userInfo.setAdmin( false );
    browseController.init( repository );
    Mockito.verify( repository ).loadRepositoryDirectoryTree( "/", "*.ktr|*.kjb", -1, false, true, true );


    userInfo.setAdmin( null );
    browseController.init( repository );
    Mockito.verify( repository, Mockito.times( 2 ) ).loadRepositoryDirectoryTree( "/", "*.ktr|*.kjb", -1, false, true, true );
  }
}
