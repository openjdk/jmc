/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * Parser extensions are a way to modify events read from a Flight Recording. It is possible to add
 * new attributes to the events and to add completely new types of events. It is also possible to
 * modify the values of existing attributes but we don't recommend that since it may break Mission
 * Control functionality.
 * <p>
 * To create a new parser extension you start by implementing the
 * {@link org.openjdk.jmc.flightrecorder.parser.IParserExtension IParserExtension} interface. This
 * is a (normally stateless) factory class that can create implementations of
 * {@link org.openjdk.jmc.flightrecorder.parser.IEventSinkFactory IEventSinkFactory}. For each
 * loading of a Flight Recording exactly one instance of IEventSinkFactory will be created for each
 * implementation of IParserExtension that is passed to the loader.
 * <p>
 * Parser extensions are loaded using the Java ServiceLoader framework. To register an
 * implementation, add its fully qualified class name to a resource file named
 * {@code org/openjdk/jmc/flightrecorder/parser/IParserExtension} that should be included in the
 * same jar file as the implementation.
 * <p>
 * The actual reading of the Flight Recording is then split into multiple threads that read
 * different parts of the recording. For every event type that is encountered a call to
 * {@link org.openjdk.jmc.flightrecorder.parser.IEventSinkFactory#create(String, String, String[], String, java.util.List)
 * IEventSinkFactory.create} is made to get an
 * {@link org.openjdk.jmc.flightrecorder.parser.IEventSink IEventSink} instance. For every event of
 * that type {@link org.openjdk.jmc.flightrecorder.parser.IEventSink#addEvent(Object[])
 * IEventSink.addEvent} is called with the actual event values. Note that the IEventSinkFactory
 * instance will be shared by all read threads so the create method must be thread safe. However,
 * each thread will have its own IEventSink instance for every event that it has found. This means
 * that there will most probably be multiple instances of IEventSink for the same event type, each
 * one owned by a separate read thread.
 * <p>
 * When all events have been read and passed through the sinks (all read threads are finished), a
 * call to {@link org.openjdk.jmc.flightrecorder.parser.IEventSinkFactory#flush()
 * IEventSinkFactory.flush} will be made. This allows the factory to create any additional events
 * that it may need to create.
 * <p>
 * The
 * {@link org.openjdk.jmc.flightrecorder.parser.IParserExtension#getEventSinkFactory(IEventSinkFactory)
 * IParserExtension.getEventSinkFactory} method takes another IEventSinkFactory instance as an
 * argument. This subfactory is the next factory to pass event types and events to. This allows for
 * creating a chain of extensions that modify the events in different ways. The last factory in this
 * chain is an internal implementation that takes care of the events for use in the Mission Control.
 * <p>
 * Basically, for every call to {@code IEventSinkfactory.create} you will get the metadata for an
 * event type. Inspect this metadata and call the {@code create} method of the subfactory with the
 * metadata, either modified or unmodified. This will give you an IEventSink instance that you can
 * return immediately if you don't want to do anything with this event, or use in your own
 * IEventSink implementation. If you want to create additional "synthetic" event types based on this
 * type, then you can call {@code create} on the subfactory multiple times.
 * <p>
 * For every call to {@code IEventSink.addEvent} you will get the event values. You can look at
 * these values and do whatever you want with them. Examples are generating additional attributes
 * and calculating statistics. Once you have your new values you call {@code addEvent} on the
 * correct sink from the subfactory. Note that the arguments to the {@code addEvent} call (the event
 * values) must match the attributes specified when the IEventSink was created with the
 * {@code create} call.
 * <p>
 * If you want to create events like statistics that should be created after all other events have
 * been read, then you can use the
 * {@link org.openjdk.jmc.flightrecorder.parser.IEventSinkFactory#flush() IEventSinkFactory.flush}
 * method. Create a sink for your new event type when you get the {@code create} call for the type
 * that you want to calculate statistics for. Collect data during {@code addEvent} calls and when
 * you get the {@code flush} call, add the statistics events by calling {@code addEvent} on the
 * statistics event sink.
 */
package org.openjdk.jmc.flightrecorder.parser;
