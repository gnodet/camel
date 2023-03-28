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
package com.sforce.eventbus;

import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.SchemaStore;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;

@org.apache.avro.specific.AvroGenerated
public class CamelEventMessage__e extends org.apache.avro.specific.SpecificRecordBase
        implements org.apache.avro.specific.SpecificRecord {
    private static final long serialVersionUID = 4603183847267960866L;

    public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse(
            "{\"type\":\"record\",\"name\":\"CamelEventMessage__e\",\"namespace\":\"com.sforce.eventbus\",\"fields\":[{\"name\":\"CreatedDate\",\"type\":\"long\",\"doc\":\"CreatedDate:DateTime\"},{\"name\":\"CreatedById\",\"type\":\"string\",\"doc\":\"CreatedBy:EntityId\"},{\"name\":\"Message__c\",\"type\":[\"null\",\"string\"],\"doc\":\"Data:Text:00NDS00000mES97\",\"default\":null}]}");

    public static org.apache.avro.Schema getClassSchema() {
        return SCHEMA$;
    }

    private static final SpecificData MODEL$ = new SpecificData();

    private static final BinaryMessageEncoder<CamelEventMessage__e> ENCODER = new BinaryMessageEncoder<>(MODEL$, SCHEMA$);

    private static final BinaryMessageDecoder<CamelEventMessage__e> DECODER = new BinaryMessageDecoder<>(MODEL$, SCHEMA$);

    /**
     * Return the BinaryMessageEncoder instance used by this class.
     *
     * @return the message encoder used by this class
     */
    public static BinaryMessageEncoder<CamelEventMessage__e> getEncoder() {
        return ENCODER;
    }

    /**
     * Return the BinaryMessageDecoder instance used by this class.
     *
     * @return the message decoder used by this class
     */
    public static BinaryMessageDecoder<CamelEventMessage__e> getDecoder() {
        return DECODER;
    }

    /**
     * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
     *
     * @param  resolver a {@link SchemaStore} used to find schemas by fingerprint
     * @return          a BinaryMessageDecoder instance for this class backed by the given SchemaStore
     */
    public static BinaryMessageDecoder<CamelEventMessage__e> createDecoder(SchemaStore resolver) {
        return new BinaryMessageDecoder<>(MODEL$, SCHEMA$, resolver);
    }

    /**
     * Serializes this CamelEventMessage__e to a ByteBuffer.
     *
     * @return                     a buffer holding the serialized data for this instance
     * @throws java.io.IOException if this instance could not be serialized
     */
    public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
        return ENCODER.encode(this);
    }

    /**
     * Deserializes a CamelEventMessage__e from a ByteBuffer.
     *
     * @param  b                   a byte buffer holding serialized data for an instance of this class
     * @return                     a CamelEventMessage__e instance decoded from the given buffer
     * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
     */
    public static CamelEventMessage__e fromByteBuffer(
            java.nio.ByteBuffer b)
            throws java.io.IOException {
        return DECODER.decode(b);
    }

    /** CreatedDate:DateTime */
    private long CreatedDate;
    /** CreatedBy:EntityId */
    private CharSequence CreatedById;
    /** Data:Text:00NDS00000mES97 */
    private CharSequence Message__c;

    /**
     * Default constructor. Note that this does not initialize fields to their default values from the schema. If that
     * is desired then one should use <code>newBuilder()</code>.
     */
    public CamelEventMessage__e() {
    }

    /**
     * All-args constructor.
     *
     * @param CreatedDate CreatedDate:DateTime
     * @param CreatedById CreatedBy:EntityId
     * @param Message__c  Data:Text:00NDS00000mES97
     */
    public CamelEventMessage__e(Long CreatedDate, CharSequence CreatedById, CharSequence Message__c) {
        this.CreatedDate = CreatedDate;
        this.CreatedById = CreatedById;
        this.Message__c = Message__c;
    }

    @Override
    public SpecificData getSpecificData() {
        return MODEL$;
    }

    @Override
    public org.apache.avro.Schema getSchema() {
        return SCHEMA$;
    }

    // Used by DatumWriter.  Applications should not call.
    @Override
    public Object get(int field$) {
        switch (field$) {
            case 0:
                return CreatedDate;
            case 1:
                return CreatedById;
            case 2:
                return Message__c;
            default:
                throw new IndexOutOfBoundsException("Invalid index: " + field$);
        }
    }

    // Used by DatumReader.  Applications should not call.
    @Override
    @SuppressWarnings(value = "unchecked")
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0:
                CreatedDate = (Long) value$;
                break;
            case 1:
                CreatedById = (CharSequence) value$;
                break;
            case 2:
                Message__c = (CharSequence) value$;
                break;
            default:
                throw new IndexOutOfBoundsException("Invalid index: " + field$);
        }
    }

    /**
     * Gets the value of the 'CreatedDate' field.
     *
     * @return CreatedDate:DateTime
     */
    public long getCreatedDate() {
        return CreatedDate;
    }

    /**
     * Sets the value of the 'CreatedDate' field. CreatedDate:DateTime
     *
     * @param value the value to set.
     */
    public void setCreatedDate(long value) {
        this.CreatedDate = value;
    }

    /**
     * Gets the value of the 'CreatedById' field.
     *
     * @return CreatedBy:EntityId
     */
    public CharSequence getCreatedById() {
        return CreatedById;
    }

    /**
     * Sets the value of the 'CreatedById' field. CreatedBy:EntityId
     *
     * @param value the value to set.
     */
    public void setCreatedById(CharSequence value) {
        this.CreatedById = value;
    }

    /**
     * Gets the value of the 'Message__c' field.
     *
     * @return Data:Text:00NDS00000mES97
     */
    public CharSequence getMessageC() {
        return Message__c;
    }

    /**
     * Sets the value of the 'Message__c' field. Data:Text:00NDS00000mES97
     *
     * @param value the value to set.
     */
    public void setMessageC(CharSequence value) {
        this.Message__c = value;
    }

    /**
     * Creates a new CamelEventMessage__e RecordBuilder.
     *
     * @return A new CamelEventMessage__e RecordBuilder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates a new CamelEventMessage__e RecordBuilder by copying an existing Builder.
     *
     * @param  other The existing builder to copy.
     * @return       A new CamelEventMessage__e RecordBuilder
     */
    public static Builder newBuilder(Builder other) {
        if (other == null) {
            return new Builder();
        } else {
            return new Builder(other);
        }
    }

    /**
     * Creates a new CamelEventMessage__e RecordBuilder by copying an existing CamelEventMessage__e instance.
     *
     * @param  other The existing instance to copy.
     * @return       A new CamelEventMessage__e RecordBuilder
     */
    public static Builder newBuilder(CamelEventMessage__e other) {
        if (other == null) {
            return new Builder();
        } else {
            return new Builder(other);
        }
    }

    /**
     * RecordBuilder for CamelEventMessage__e instances.
     */
    @org.apache.avro.specific.AvroGenerated
    public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<CamelEventMessage__e>
            implements org.apache.avro.data.RecordBuilder<CamelEventMessage__e> {

        /** CreatedDate:DateTime */
        private long CreatedDate;
        /** CreatedBy:EntityId */
        private CharSequence CreatedById;
        /** Data:Text:00NDS00000mES97 */
        private CharSequence Message__c;

        /** Creates a new Builder */
        private Builder() {
            super(SCHEMA$, MODEL$);
        }

        /**
         * Creates a Builder by copying an existing Builder.
         *
         * @param other The existing Builder to copy.
         */
        private Builder(Builder other) {
            super(other);
            if (isValidValue(fields()[0], other.CreatedDate)) {
                this.CreatedDate = data().deepCopy(fields()[0].schema(), other.CreatedDate);
                fieldSetFlags()[0] = other.fieldSetFlags()[0];
            }
            if (isValidValue(fields()[1], other.CreatedById)) {
                this.CreatedById = data().deepCopy(fields()[1].schema(), other.CreatedById);
                fieldSetFlags()[1] = other.fieldSetFlags()[1];
            }
            if (isValidValue(fields()[2], other.Message__c)) {
                this.Message__c = data().deepCopy(fields()[2].schema(), other.Message__c);
                fieldSetFlags()[2] = other.fieldSetFlags()[2];
            }
        }

        /**
         * Creates a Builder by copying an existing CamelEventMessage__e instance
         *
         * @param other The existing instance to copy.
         */
        private Builder(CamelEventMessage__e other) {
            super(SCHEMA$, MODEL$);
            if (isValidValue(fields()[0], other.CreatedDate)) {
                this.CreatedDate = data().deepCopy(fields()[0].schema(), other.CreatedDate);
                fieldSetFlags()[0] = true;
            }
            if (isValidValue(fields()[1], other.CreatedById)) {
                this.CreatedById = data().deepCopy(fields()[1].schema(), other.CreatedById);
                fieldSetFlags()[1] = true;
            }
            if (isValidValue(fields()[2], other.Message__c)) {
                this.Message__c = data().deepCopy(fields()[2].schema(), other.Message__c);
                fieldSetFlags()[2] = true;
            }
        }

        /**
         * Gets the value of the 'CreatedDate' field. CreatedDate:DateTime
         *
         * @return The value.
         */
        public long getCreatedDate() {
            return CreatedDate;
        }

        /**
         * Sets the value of the 'CreatedDate' field. CreatedDate:DateTime
         *
         * @param  value The value of 'CreatedDate'.
         * @return       This builder.
         */
        public Builder setCreatedDate(long value) {
            validate(fields()[0], value);
            this.CreatedDate = value;
            fieldSetFlags()[0] = true;
            return this;
        }

        /**
         * Checks whether the 'CreatedDate' field has been set. CreatedDate:DateTime
         *
         * @return True if the 'CreatedDate' field has been set, false otherwise.
         */
        public boolean hasCreatedDate() {
            return fieldSetFlags()[0];
        }

        /**
         * Clears the value of the 'CreatedDate' field. CreatedDate:DateTime
         *
         * @return This builder.
         */
        public Builder clearCreatedDate() {
            fieldSetFlags()[0] = false;
            return this;
        }

        /**
         * Gets the value of the 'CreatedById' field. CreatedBy:EntityId
         *
         * @return The value.
         */
        public CharSequence getCreatedById() {
            return CreatedById;
        }

        /**
         * Sets the value of the 'CreatedById' field. CreatedBy:EntityId
         *
         * @param  value The value of 'CreatedById'.
         * @return       This builder.
         */
        public Builder setCreatedById(CharSequence value) {
            validate(fields()[1], value);
            this.CreatedById = value;
            fieldSetFlags()[1] = true;
            return this;
        }

        /**
         * Checks whether the 'CreatedById' field has been set. CreatedBy:EntityId
         *
         * @return True if the 'CreatedById' field has been set, false otherwise.
         */
        public boolean hasCreatedById() {
            return fieldSetFlags()[1];
        }

        /**
         * Clears the value of the 'CreatedById' field. CreatedBy:EntityId
         *
         * @return This builder.
         */
        public Builder clearCreatedById() {
            CreatedById = null;
            fieldSetFlags()[1] = false;
            return this;
        }

        /**
         * Gets the value of the 'Message__c' field. Data:Text:00NDS00000mES97
         *
         * @return The value.
         */
        public CharSequence getMessageC() {
            return Message__c;
        }

        /**
         * Sets the value of the 'Message__c' field. Data:Text:00NDS00000mES97
         *
         * @param  value The value of 'Message__c'.
         * @return       This builder.
         */
        public Builder setMessageC(CharSequence value) {
            validate(fields()[2], value);
            this.Message__c = value;
            fieldSetFlags()[2] = true;
            return this;
        }

        /**
         * Checks whether the 'Message__c' field has been set. Data:Text:00NDS00000mES97
         *
         * @return True if the 'Message__c' field has been set, false otherwise.
         */
        public boolean hasMessageC() {
            return fieldSetFlags()[2];
        }

        /**
         * Clears the value of the 'Message__c' field. Data:Text:00NDS00000mES97
         *
         * @return This builder.
         */
        public Builder clearMessageC() {
            Message__c = null;
            fieldSetFlags()[2] = false;
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public CamelEventMessage__e build() {
            try {
                CamelEventMessage__e record = new CamelEventMessage__e();
                record.CreatedDate = fieldSetFlags()[0] ? this.CreatedDate : (Long) defaultValue(fields()[0]);
                record.CreatedById = fieldSetFlags()[1] ? this.CreatedById : (CharSequence) defaultValue(fields()[1]);
                record.Message__c = fieldSetFlags()[2] ? this.Message__c : (CharSequence) defaultValue(fields()[2]);
                return record;
            } catch (org.apache.avro.AvroMissingFieldException e) {
                throw e;
            } catch (Exception e) {
                throw new org.apache.avro.AvroRuntimeException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static final org.apache.avro.io.DatumWriter<CamelEventMessage__e> WRITER$
            = (org.apache.avro.io.DatumWriter<CamelEventMessage__e>) MODEL$.createDatumWriter(SCHEMA$);

    @Override
    public void writeExternal(java.io.ObjectOutput out)
            throws java.io.IOException {
        WRITER$.write(this, SpecificData.getEncoder(out));
    }

    @SuppressWarnings("unchecked")
    private static final org.apache.avro.io.DatumReader<CamelEventMessage__e> READER$
            = (org.apache.avro.io.DatumReader<CamelEventMessage__e>) MODEL$.createDatumReader(SCHEMA$);

    @Override
    public void readExternal(java.io.ObjectInput in)
            throws java.io.IOException {
        READER$.read(this, SpecificData.getDecoder(in));
    }

    @Override
    protected boolean hasCustomCoders() {
        return true;
    }

    @Override
    public void customEncode(org.apache.avro.io.Encoder out)
            throws java.io.IOException {
        out.writeLong(this.CreatedDate);

        out.writeString(this.CreatedById);

        if (this.Message__c == null) {
            out.writeIndex(0);
            out.writeNull();
        } else {
            out.writeIndex(1);
            out.writeString(this.Message__c);
        }

    }

    @Override
    public void customDecode(org.apache.avro.io.ResolvingDecoder in)
            throws java.io.IOException {
        org.apache.avro.Schema.Field[] fieldOrder = in.readFieldOrderIfDiff();
        if (fieldOrder == null) {
            this.CreatedDate = in.readLong();

            this.CreatedById = in.readString(this.CreatedById instanceof Utf8 ? (Utf8) this.CreatedById : null);

            if (in.readIndex() != 1) {
                in.readNull();
                this.Message__c = null;
            } else {
                this.Message__c = in.readString(this.Message__c instanceof Utf8 ? (Utf8) this.Message__c : null);
            }

        } else {
            for (int i = 0; i < 3; i++) {
                switch (fieldOrder[i].pos()) {
                    case 0:
                        this.CreatedDate = in.readLong();
                        break;

                    case 1:
                        this.CreatedById = in.readString(this.CreatedById instanceof Utf8 ? (Utf8) this.CreatedById : null);
                        break;

                    case 2:
                        if (in.readIndex() != 1) {
                            in.readNull();
                            this.Message__c = null;
                        } else {
                            this.Message__c = in.readString(this.Message__c instanceof Utf8 ? (Utf8) this.Message__c : null);
                        }
                        break;

                    default:
                        throw new java.io.IOException("Corrupt ResolvingDecoder.");
                }
            }
        }
    }
}
