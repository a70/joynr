/*
 * #%L
 * %%
 * Copyright (C) 2011 - 2015 BMW Car IT GmbH
 * %%
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
 * #L%
 */
#include "JoynrMessageSerializer.h"

#include "joynr/ArraySerializer.h"
#include "joynr/SerializerRegistry.h"
#include "joynr/Variant.h"
#include "joynr/JoynrTypeId.h"

#include <string>
#include <utility>

namespace joynr
{

// Register the Request type id and serializer/deserializer
static const bool isRequestRegistered =
        SerializerRegistry::registerType<JoynrMessage>("joynr.JoynrMessage");

static std::string  removeEscapeFromSpecialChars(const std::string& inputStr){
    const std::string escapedQuote = R"(\")";
    const std::string normalQuote = R"(")";

    std::string unescapedString{inputStr};
    std::string::size_type n = 0;
    while ( ( n = unescapedString.find( escapedQuote, n ) ) != std::string::npos )
    {
        unescapedString.replace( n, escapedQuote.size(), normalQuote );
        n += normalQuote.size();
    }

    return unescapedString;
}

template <>
void ClassDeserializer<JoynrMessage>::deserialize(JoynrMessage& t, IObject& o)
{
    while (o.hasNextField()) {
        IField& field = o.nextField();
        if (field.name() == "type") {
            t.setType(field.value());
        } else if (field.name() == "headerMap") {
            auto&& converted = convertMap<std::string>(field.value(), convertString);
            t.setHeaderMap(converted);
        } else if (field.name() == "payload") {
            t.setPayload( removeEscapeFromSpecialChars(field.value()));
        }
    }
}

template <>
void ClassSerializer<JoynrMessage>::serialize(const JoynrMessage& request, std::ostream& stream)
{
    stream << "{";
    stream << "\"_typeName\": \"" << JoynrTypeId<JoynrMessage>::getTypeName() << "\",";
    stream << "\"headerMap\": ";
    // ArraySerializer::serialize<Variant>(request.getParams(), stream);
    // ArraySerializer::serializeStrings(xxx, stream);
    // ClassSerializer<SomeObject>::serialize(object, stream);
    stream << "}";
}

} /* namespace joynr */
