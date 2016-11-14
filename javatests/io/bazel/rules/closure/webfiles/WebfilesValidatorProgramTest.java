// Copyright 2016 The Closure Rules Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.bazel.rules.closure.webfiles;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Jimfs;
import io.bazel.rules.closure.webfiles.BuildInfo.Webfiles;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/** Unit tests for {@link WebfilesValidatorProgram}. */
@RunWith(JUnit4.class)
public class WebfilesValidatorProgramTest {

  private final FileSystem fs = Jimfs.newFileSystem();
  private final PrintStream output = mock(PrintStream.class);
  private final WebfilesValidator validator = mock(WebfilesValidator.class);
  private final WebfilesValidatorProgram program =
      new WebfilesValidatorProgram(output, fs, validator);

  @After
  public void guaranteeStrictMocking() throws Exception {
    verifyNoMoreInteractions(output, validator);
  }

  @Test
  public void noArgs_showsError() throws Exception {
    assertThat(program.apply(ImmutableList.<String>of())).isEqualTo(1);
    verify(output).println(contains("Missing --target flag"));
  }

  @Test
  public void targetFlag_loadsProtoAndPassesAlong() throws Exception {
    save(fs.getPath("/a.pbtxt"), "label: \"@oh//my:goth\"\n");
    when(validator.validate(
            any(Webfiles.class),
            Mockito.<Iterable<Webfiles>>any(),
            Mockito.<Supplier<Iterable<Webfiles>>>any()))
        .thenReturn(ImmutableList.<String>of());
    assertThat(program.apply(ImmutableList.of("--target", "/a.pbtxt"))).isEqualTo(0);
    verify(validator).validate(
        eq(Webfiles.newBuilder().setLabel("@oh//my:goth").build()),
        eq(ImmutableList.<Webfiles>of()),
        Mockito.<Supplier<Iterable<Webfiles>>>any());
  }

  @Test
  public void validatorReturnsError_getsPrintedAndReturnsNonZero() throws Exception {
    save(fs.getPath("/a.pbtxt"), "label: \"@oh//my:goth\"\n");
    when(validator.validate(
            any(Webfiles.class),
            Mockito.<Iterable<Webfiles>>any(),
            Mockito.<Supplier<Iterable<Webfiles>>>any()))
        .thenReturn(ImmutableList.of("hey listen"));
    assertThat(program.apply(ImmutableList.of("--target", "/a.pbtxt"))).isEqualTo(1);
    verify(validator).validate(
        eq(Webfiles.newBuilder().setLabel("@oh//my:goth").build()),
        eq(ImmutableList.<Webfiles>of()),
        Mockito.<Supplier<Iterable<Webfiles>>>any());
    verify(output).println(contains("hey listen"));
  }

  private void save(Path path, String contents) throws IOException {
    Files.createDirectories(path.getParent());
    Files.write(path, contents.getBytes(UTF_8));
  }
}
