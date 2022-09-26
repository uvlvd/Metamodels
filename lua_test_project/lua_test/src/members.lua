

Foo = {
	bar =  function(...)
	end
}

-- Aliasing assignment
foo = Foo
foo.bar()
foo:bar()

result = foo.bar

-- These two calls are synonymous
Foo.bar(foo, "blub")
Foo:bar("blub")