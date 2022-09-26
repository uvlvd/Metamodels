

Foo, Bar = {
	bar = {
		baz = function(...)
		end
	}
}, {
	blub = 43
}

foo = Foo

-- These two calls are synonymou
Foo.bar.baz(foo, "blub")
foo.bar:bar("blub")