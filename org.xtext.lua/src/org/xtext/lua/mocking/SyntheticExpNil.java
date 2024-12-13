package org.xtext.lua.mocking;

import org.xtext.lua.lua.impl.ExpNilImpl;

public class SyntheticExpNil extends ExpNilImpl {
	// This class is only used to differentiate between ExpNils found in the code and ExpNils added synthetically, e.g.
	// in Assignments with less Exps than Vars: a, b = 1
}
