

Foo = {
	bar =  function(...)
	end
}

-- Aliasing assignment
foo = Foo
foo.bar()

-- These two calls are synonymous
Foo.bar(foo, "blub")
foo:bar("blub")