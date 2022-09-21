

Foo = {
	bar = function(...) end
}

foo = Foo

-- These two calls are synonymou
Foo.bar(foo, "blub")
foo:bar("blub")