/* Licensed under the Apache License, Version 2.0 (the "License");
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
 */
package org.camunda.bpm.extension.osgi.scripting.impl;

import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.scripting.ScriptBindingsFactory;
import org.camunda.bpm.engine.impl.scripting.ScriptingEngines;
import org.camunda.bpm.extension.osgi.scripting.ScriptEngineResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * @author Tijs Rademakers
 */
public class OsgiScriptingEngines extends ScriptingEngines {

	private static final Logger LOGGER = Logger
			.getLogger(OsgiScriptingEngines.class.getName());

	private Bundle bundle = FrameworkUtil.getBundle(getClass());

	public OsgiScriptingEngines(ScriptBindingsFactory scriptBindingsFactory) {
		super(scriptBindingsFactory);
	}

	public Object evaluate(String script, String language,
			VariableScope variableScope) {
		Bindings bindings = createBindings(variableScope);
		ScriptEngine scriptEngine = null;
		try {
			scriptEngine = resolveScriptEngine(language);
		} catch (InvalidSyntaxException e) {
			throw new ProcessEngineException(
					"problem resolving scripting engine" + e.getMessage(), e);
		}

		if (scriptEngine == null) {
			throw new ProcessEngineException(
					"Can't find scripting engine for '" + language + "'");
		}

		try {
			return scriptEngine.eval(script, bindings);
		} catch (ScriptException e) {
			throw new ProcessEngineException("problem evaluating script: "
					+ e.getMessage(), e);
		}
	}

	/**
	 * test-only helper method
	 */
	void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	ScriptEngine resolveScriptEngine(String scriptEngineName)
			throws InvalidSyntaxException {
		BundleContext context = bundle.getBundleContext();
		ServiceReference[] refs = context.getServiceReferences(
				ScriptEngineResolver.class.getName(), null);
		if (refs == null) {
			LOGGER.info("No OSGi script engine resolvers available!");
			return null;
		}

		LOGGER.fine("Found " + refs.length
				+ " OSGi ScriptEngineResolver services");

		for (ServiceReference ref : refs) {
			ScriptEngineResolver resolver = (ScriptEngineResolver) context
					.getService(ref);
			ScriptEngine engine = resolver
					.resolveScriptEngine(scriptEngineName);
			context.ungetService(ref);
			LOGGER.fine("OSGi resolver " + resolver + " produced "
					+ scriptEngineName + " engine " + engine);
			if (engine != null) {
				return engine;
			}
		}
		return null;
	}
}
