// Copyright (c) 2018, Yubico AB
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.yubico.webauthn

import java.security.interfaces.ECPublicKey

import com.yubico.webauthn.data.ByteArray
import org.junit.runner.RunWith
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalatest.Matchers
import org.scalatest.FunSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import scala.util.Try


@RunWith(classOf[JUnitRunner])
class WebAuthnCodecsSpec  extends FunSpec with Matchers with GeneratorDrivenPropertyChecks {

  private val javaCryptoProvider: java.security.Provider = new BouncyCastleCrypto().getProvider

  implicit def arbitraryEcPublicKey: Arbitrary[ECPublicKey] = Arbitrary(
    for {
      ySign: Byte <- Gen.oneOf(0x02: Byte, 0x03: Byte)
      rawBytes: Seq[Byte] <- Gen.listOfN[Byte](32, Arbitrary.arbitrary[Byte])
      key = Try(new BouncyCastleCrypto().decodePublicKey(new ByteArray((ySign +: rawBytes).toArray)).asInstanceOf[ECPublicKey])
      if key.isSuccess
    } yield key.get
  )

  describe("The ecPublicKeyToRaw method") {

    it("outputs the correct x and y values") {
      forAll (minSuccessful(500)) { pubkey: ECPublicKey =>
        val rawkey: Array[Byte] = WebAuthnCodecs.ecPublicKeyToRaw(pubkey).getBytes

        rawkey.length should equal (65)
        rawkey(0) should equal (0x04: Byte)

        val x = rawkey.slice(1, 33)
        val y = rawkey.slice(33, 65)

        val expectedX = pubkey.getW.getAffineX.toByteArray.toVector
        val expectedY = pubkey.getW.getAffineY.toByteArray.toVector

        x.dropWhile(_ == (0: Byte)) should equal (expectedX.dropWhile(_ == (0: Byte)))
        y.dropWhile(_ == (0: Byte)) should equal (expectedY.dropWhile(_ == (0: Byte)))
      }
    }

  }

  describe("The rawEcdaKeyToCose method") {

    it("outputs a value that can be imported by importCoseP256PublicKey") {
      forAll { originalPubkey: ECPublicKey =>
        val rawKey = WebAuthnCodecs.ecPublicKeyToRaw(originalPubkey)

        val coseKey = WebAuthnTestCodecs.rawEcdaKeyToCose(rawKey)

        val importedPubkey: ECPublicKey = WebAuthnCodecs.importCosePublicKey(coseKey).asInstanceOf[ECPublicKey]
        val rawImportedPubkey = WebAuthnCodecs.ecPublicKeyToRaw(importedPubkey)

        rawImportedPubkey should equal (rawKey)
      }
    }

  }

  describe("The ecPublicKeyToCose method") {

    it("outputs a value that can be imported by importCoseP256PublicKey") {
      forAll { originalPubkey: ECPublicKey =>
        val rawKey = WebAuthnCodecs.ecPublicKeyToRaw(originalPubkey)

        val coseKey = WebAuthnTestCodecs.ecPublicKeyToCose(originalPubkey)

        val importedPubkey: ECPublicKey = WebAuthnCodecs.importCosePublicKey(coseKey).asInstanceOf[ECPublicKey]
        val rawImportedPubkey = WebAuthnCodecs.ecPublicKeyToRaw(importedPubkey)

        rawImportedPubkey should equal (rawKey)
      }
    }

  }

}
