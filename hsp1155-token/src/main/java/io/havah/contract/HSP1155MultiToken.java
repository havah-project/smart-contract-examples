/*
 * Copyright 2022 HAVAH Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.havah.contract;

import io.havah.contract.token.hsp1155.HSP1155MintBurn;
import score.annotation.External;

public class HSP1155MultiToken extends HSP1155MintBurn {
    private final String name;
    public HSP1155MultiToken(String _name) {
        name = _name;
    }

    @External(readonly=true)
    public String name() {
        return name;
    }
}
