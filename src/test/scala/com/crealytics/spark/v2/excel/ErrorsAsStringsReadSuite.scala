/** Copyright 2016 - 2021 Martin Mauch (@nightscape)
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
  * the License. You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
  * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
  * specific language governing permissions and limitations under the License.
  */
package com.crealytics.spark.v2.excel

import com.holdenkarau.spark.testing.DataFrameSuiteBase
import org.apache.spark.sql.Row
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.scalatest.funsuite.AnyFunSuite

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util
import scala.collection.JavaConverters._

object ErrorsAsStringsReadSuite {
  private val dummyTimestamp = Timestamp.valueOf(LocalDateTime.of(2021, 2, 19, 0, 0))
  private val dummyText = "hello"

  private val expectedSchemaInfer = StructType(
    List(
      StructField("double", IntegerType, true),
      StructField("boolean", BooleanType, true),
      StructField("timestamp", TimestampType, true),
      StructField("string", StringType, true)
    )
  )

  private val expectedDataErrorsAsNullInfer: util.List[Row] = List(
    Row(1, true, dummyTimestamp, dummyText),
    Row(2, false, dummyTimestamp, dummyText),
    Row(null, null, null, null),
    Row(null, null, null, null)
  ).asJava

  private val expectedDataErrorsAsStringsInfer: util.List[Row] = List(
    Row(1, true, dummyTimestamp, dummyText),
    Row(2, false, dummyTimestamp, dummyText),
    Row(null, null, null, "#NULL!"),
    Row(null, null, null, "#N/A")
  ).asJava

  private val expectedSchemaNonInfer = StructType(
    List(
      StructField("double", StringType, true),
      StructField("boolean", StringType, true),
      StructField("timestamp", StringType, true),
      StructField("string", StringType, true)
    )
  )

  private val expectedDataErrorsAsNullNonInfer: util.List[Row] = List(
    Row("1", "TRUE", """19"-"Feb"-"2021""", "hello"),
    Row("2", "FALSE", """19"-"Feb"-"2021""", "hello"),
    Row(null, null, null, null),
    Row(null, null, null, null)
  ).asJava

  private val expectedDataErrorsAsStringsNonInfer: util.List[Row] = List(
    Row("1", "TRUE", """19"-"Feb"-"2021""", dummyText),
    Row("2", "FALSE", """19"-"Feb"-"2021""", dummyText),
    Row("#NULL!", "#NULL!", "#NULL!", "#NULL!"),
    Row("#N/A", "#N/A", "#N/A", "#N/A")
  ).asJava
}

/** Breaking change with V1: For Spark String Type field, Error Cell has an option to either get original-formual-string
  * or null as any other Spark Types
  *
  * Related issues: Support ERROR cell type when using inferSchema=true link:
  * https://github.com/crealytics/spark-excel/pull/343
  */
class ErrorsAsStringsReadSuite extends AnyFunSuite with DataFrameSuiteBase with ExcelTestingUtilities {
  import ErrorsAsStringsReadSuite._

  test("error cells as null when useNullForErrorCells=true and inferSchema=true") {
    val df = readFromResources(
      spark,
      path = "with_errors_all_types.xlsx",
      options = Map("inferSchema" -> true, "useNullForErrorCells" -> true)
    )
    val expected = spark.createDataFrame(expectedDataErrorsAsNullInfer, expectedSchemaInfer)
    assertDataFrameEquals(expected, df)
  }

  test("errors as null for non-string type with useNullForErrorCells=false and inferSchema=true") {
    val df = readFromResources(
      spark,
      path = "with_errors_all_types.xlsx",
      options = Map("inferSchema" -> true, "useNullForErrorCells" -> false)
    )
    val expected = spark.createDataFrame(expectedDataErrorsAsStringsInfer, expectedSchemaInfer)
    assertDataFrameEquals(expected, df)
  }

  test("errors in string format when useNullForErrorCells=true and inferSchema=false") {
    val df = readFromResources(
      spark,
      path = "with_errors_all_types.xlsx",
      options = Map("inferSchema" -> false, "useNullForErrorCells" -> true)
    )
    val expected = spark.createDataFrame(expectedDataErrorsAsNullNonInfer, expectedSchemaNonInfer)
    assertDataFrameEquals(expected, df)
  }

  test("errors in string format when useNullForErrorCells=false and inferSchema=false") {
    val df = readFromResources(
      spark,
      path = "with_errors_all_types.xlsx",
      options = Map("inferSchema" -> false, "useNullForErrorCells" -> false)
    )
    val expected = spark
      .createDataFrame(expectedDataErrorsAsStringsNonInfer, expectedSchemaNonInfer)
    assertDataFrameEquals(expected, df)
  }

}
